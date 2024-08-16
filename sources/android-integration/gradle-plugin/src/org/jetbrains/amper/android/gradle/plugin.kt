/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.amper.android.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import kotlinx.serialization.json.Json
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.gradle.api.provider.Property
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.amper.android.AndroidBuildRequest
import org.jetbrains.amper.android.gradle.tooling.ProcessResourcesProviderTaskNameToolingModelBuilder
import org.jetbrains.amper.core.Result
import org.jetbrains.amper.frontend.LeafFragment
import org.jetbrains.amper.frontend.Model
import org.jetbrains.amper.frontend.Platform
import org.jetbrains.amper.frontend.PotatoModule
import org.jetbrains.amper.frontend.PotatoModuleDependency
import org.jetbrains.amper.frontend.PotatoModuleFileSource
import org.jetbrains.amper.frontend.aomBuilder.SchemaBasedModelImport
import org.jetbrains.amper.frontend.project.StandaloneAmperProjectContext
import org.jetbrains.amper.frontend.schema.ProductType
import java.io.File
import java.nio.file.Path
import java.util.*
import javax.inject.Inject
import javax.xml.stream.XMLEventFactory
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isSameFileAs
import kotlin.io.path.pathString
import kotlin.io.path.reader
import kotlin.io.path.relativeTo


interface AmperAndroidIntegrationExtension {
    val jsonData: Property<String>
}

private const val PROJECT_TO_MODULE_EXT = "org.jetbrains.amper.gradle.android.ext.projectToModule"
private const val MODULE_TO_PROJECT_EXT = "org.jetbrains.amper.gradle.android.ext.moduleToProject"
private const val ANDROID_REQUEST = "org.jetbrains.amper.gradle.android.ext.androidRequest"
private const val KNOWN_MODEL_EXT = "org.jetbrains.amper.gradle.android.ext.model"

fun <K, V> ExtraPropertiesExtension.getBindingMap(name: String) = try {
    this[name] as MutableMap<K, V>
} catch (_: ExtraPropertiesExtension.UnknownPropertyException) {
    val bindingMap = mutableMapOf<K, V>()
    this[name] = bindingMap
    bindingMap
}

val Gradle.projectPathToModule: MutableMap<String, PotatoModule>
    get() = (this as ExtensionAware).extensions.extraProperties.getBindingMap(PROJECT_TO_MODULE_EXT)

val Gradle.moduleFilePathToProject: MutableMap<Path, String>
    get() = (this as ExtensionAware).extensions.extraProperties.getBindingMap(MODULE_TO_PROJECT_EXT)

var Gradle.request: AndroidBuildRequest?
    get() = (this as ExtensionAware).extensions.extraProperties.get(ANDROID_REQUEST) as? AndroidBuildRequest
    set(value) = (this as ExtensionAware).extensions.extraProperties.set(ANDROID_REQUEST, value)

var Gradle.knownModel: Model?
    get() = (this as ExtensionAware).extensions.extraProperties[KNOWN_MODEL_EXT] as? Model
    set(value) {
        (this as ExtensionAware).extensions.extraProperties[KNOWN_MODEL_EXT] = value
    }


val PotatoModule.buildFile get() = (source as PotatoModuleFileSource).buildFile

val PotatoModule.buildDir get() = buildFile.parent

private const val SIGNING_CONFIG_NAME = "sign"

@Suppress("UnstableApiUsage")
class AmperAndroidIntegrationProjectPlugin @Inject constructor(private val problems: Problems) : Plugin<Project> {
    override fun apply(project: Project): Unit = with(SLF4JProblemReporterContext()) {
        val log = project.logger
        val rootProjectBuildDir = project.rootProject.layout.buildDirectory.asFile.get().toPath()
        val buildDir = rootProjectBuildDir / project.path.replace(":", "_")
        project.layout.buildDirectory.set(buildDir.toFile())
        project.repositories.google()
        project.repositories.mavenCentral()
        val module = project.gradle.projectPathToModule[project.path] ?: return

        if (module.type != ProductType.ANDROID_APP) {
            error("Unsupported module type: ${module.type}")
        }

        project.plugins.apply("com.android.application")

        val androidExtension = project.extensions.findByType(BaseExtension::class.java) ?: return
        val androidFragment = module
            .fragments
            .filterIsInstance<LeafFragment>()
            .firstOrNull { it.platforms.contains(Platform.ANDROID) } ?: return

        val androidSettings = androidFragment.settings.android
        androidExtension.compileSdkVersion(androidSettings.compileSdk.versionNumber)

        val signing = androidSettings.signing

        if (signing.enabled) {
            val path = (module.buildDir / signing.propertiesFile.pathString).normalize().absolute()
            if (path.exists()) {
                val keystoreProperties = Properties().apply {
                    path.reader().use { reader ->
                        load(reader)
                    }
                }
                androidExtension.signingConfigs {
                    it.create(SIGNING_CONFIG_NAME) {
                        it.storeFile = Path(keystoreProperties.getProperty(signing.storeFileKey)).toFile()
                        it.storePassword = keystoreProperties.getProperty(signing.storePasswordKey)
                        it.keyAlias = keystoreProperties.getProperty(signing.keyAliasKey)
                        it.keyPassword = keystoreProperties.getProperty(signing.keyPasswordKey)
                    }
                }
            } else {
                problems
                    .forNamespace("org.jetbrains.amper.android-integration")
                    .reporting { problem ->
                        problem
                            .id("signing-properties-file-not-found", "Signing properties file not found")
                            .contextualLabel("Signing properties file not found")
                            .details("Signing properties file $path not found. Signing will not be configured")
                            .severity(Severity.WARNING)
                            .solution("Put signing properties file to $path")
                }
                log.warn("Properties file $path not found. Signing will not be configured")
            }
        }

        androidExtension.defaultConfig {
            it.maxSdk = androidSettings.maxSdk.versionNumber
            it.targetSdk = androidSettings.targetSdk.versionNumber
            it.minSdk = androidSettings.minSdk.versionNumber
            it.versionCode = 1
            if (module.type == ProductType.ANDROID_APP) {
                it.applicationId = androidSettings.applicationId
            }
            androidExtension.signingConfigs.findByName(SIGNING_CONFIG_NAME)?.let { signing ->
                it.signingConfig = signing
            }
        }
        androidExtension.namespace = androidSettings.namespace

        val requestedModules = project
            .gradle
            .request
            ?.modules
            ?.associate { it.modulePath to it } ?: mapOf()

        androidExtension.sourceSets.matching { it.name == "main" }.all {
            it.manifest.srcFile(androidFragment.src.resolve("AndroidManifest.xml"))
            it.res.setSrcDirs(setOf(module.buildDir.resolve("res")))
        }

        project.afterEvaluate {

            // get variants
            val variants = (androidExtension as AppExtension).applicationVariants
            // choose variant
            val buildTypes = (project.gradle.request?.buildTypes ?: emptySet()).map { it.value }.toSet()
            val chosenVariants = variants.filter { it.name in buildTypes }

            for (variant in chosenVariants) {
                val requestedModule = requestedModules[project.path] ?: return@afterEvaluate

                // set dependencies
                for (dependency in requestedModule.resolvedAndroidRuntimeDependencies) {
                    variant.runtimeConfiguration.dependencies.add(
                        ResolvedAmperDependency(
                            project,
                            dependency
                        )
                    )
                }

                // set inter-module dependencies between android modules
                val androidDependencyPaths = project.gradle.knownModel?.let { model ->
                    androidFragment
                        .externalDependencies
                        .asSequence()
                        .filterIsInstance<PotatoModuleDependency>()
                        .map { it.module }
                        .filter { it.artifacts.any { Platform.ANDROID in it.platforms } }
                        .mapNotNull { project.gradle.moduleFilePathToProject[it.buildDir] }
                        .filter { it in requestedModules }
                        .toList()
                } ?: listOf()

                for (path in androidDependencyPaths) {
                    variant.runtimeConfiguration.dependencies.add(project.dependencies.project(mapOf("path" to path)))
                }

                // set classes
                requestedModule.moduleClasses.forEach {
                    variant.registerPostJavacGeneratedBytecode(project.files(it))
                }
            }
        }
    }
}

class AmperAndroidIntegrationSettingsPlugin @Inject constructor(private val toolingModelBuilderRegistry: ToolingModelBuilderRegistry) :
    Plugin<Settings> {
    override fun apply(settings: Settings) = with(SLF4JProblemReporterContext()) {
        toolingModelBuilderRegistry.register(ProcessResourcesProviderTaskNameToolingModelBuilder())
        val extension = settings.extensions.create("androidData", AmperAndroidIntegrationExtension::class.java)

        settings.gradle.settingsEvaluated {
            val request = Json.decodeFromString<AndroidBuildRequest>(extension.jsonData.get())
            settings.gradle.request = request
            initProjects(request.root, settings)
        }

        settings.gradle.beforeProject { project ->
            adjustXmlFactories()
            settings.gradle.projectPathToModule[project.path]?.let { module ->
                if (module.type == ProductType.ANDROID_APP) {
                    project.plugins.apply(AmperAndroidIntegrationProjectPlugin::class.java)
                }
            }
        }
    }

    context(SLF4JProblemReporterContext)
    private fun initProjects(projectRoot: Path, settings: Settings) {
        // TODO Instead of importing the Amper model, we could pass the information we need from the Amper CLI.
        //   The interface between the Amper CLI and the Gradle delegate project would be more clearly defined,
        //   and we could use just the relevant subset of the data.
        //   Some pieces of data might even have already been resolved/changed in the Amper CLI, such as dependencies.
        //   and in that case we wouldn't want Gradle to re-read the Amper model files and get it wrong.
        //   Also, it would avoid parsing all modules files in the entire project for each delegated Gradle build.
        val projectContext = StandaloneAmperProjectContext.create(projectRoot, project = null)
            ?: error("Invalid project root passed to the delegated Android Gradle build: $projectRoot")
        val model = when (val result = SchemaBasedModelImport.getModel(projectContext)) {
            is Result.Failure -> throw result.exception
            is Result.Success -> result.value
        }

        settings.gradle.knownModel = model

        val rootPath = projectRoot.normalize().toAbsolutePath()
        val androidModules = model
            .modules
            .filter {
                val productTypeIsAndroidApp = it.type == ProductType.ANDROID_APP
                val productTypeIsLib = it.type == ProductType.LIB
                val platformsContainAndroid = it.artifacts.any { it.platforms.contains(Platform.ANDROID) }
                productTypeIsAndroidApp || productTypeIsLib && platformsContainAndroid
            }
            .sortedBy { it.buildFile }

        fun Path.toGradlePath() = ":" + relativeTo(rootPath).toString().replace(File.separator, ":")

        androidModules.forEach {
            val currentPath = it.buildDir.normalize().toAbsolutePath()
            val projectPath = if (currentPath.isSameFileAs(rootPath)) {
                ":"
            } else {
                currentPath.toGradlePath()
            }
            if (projectPath != ":") {
                settings.include(projectPath)
                val project = settings.project(projectPath)
                project.projectDir = it.buildDir.toFile()
            }

            settings.gradle.projectPathToModule[projectPath] = it
            settings.gradle.moduleFilePathToProject[it.buildDir] = projectPath
        }
    }
}

fun trySetSystemProperty(key: String, value: String) {
    if (System.getProperty(key) == null)
        System.setProperty(key, value)
}

fun adjustXmlFactories() {
    trySetSystemProperty(
        XMLInputFactory::class.qualifiedName!!,
        "com.sun.xml.internal.stream.XMLInputFactoryImpl"
    )
    trySetSystemProperty(
        XMLOutputFactory::class.qualifiedName!!,
        "com.sun.xml.internal.stream.XMLOutputFactoryImpl"
    )
    trySetSystemProperty(
        XMLEventFactory::class.qualifiedName!!,
        "com.sun.xml.internal.stream.events.XMLEventFactoryImpl"
    )
}
