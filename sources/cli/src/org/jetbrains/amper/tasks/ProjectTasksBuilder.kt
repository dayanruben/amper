package org.jetbrains.amper.tasks

import org.jetbrains.amper.cli.CliProblemReporterContext
import org.jetbrains.amper.cli.ProjectContext
import org.jetbrains.amper.cli.TaskGraphBuilder
import org.jetbrains.amper.core.get
import org.jetbrains.amper.engine.Task
import org.jetbrains.amper.engine.TaskGraph
import org.jetbrains.amper.engine.TaskName
import org.jetbrains.amper.frontend.AndroidPart
import org.jetbrains.amper.frontend.MavenDependency
import org.jetbrains.amper.frontend.Model
import org.jetbrains.amper.frontend.Platform
import org.jetbrains.amper.frontend.PotatoModule
import org.jetbrains.amper.frontend.PotatoModuleDependency
import org.jetbrains.amper.frontend.PotatoModuleFileSource
import org.jetbrains.amper.util.ExecuteOnChangedInputs
import org.jetbrains.amper.util.targetLeafPlatforms
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.io.path.exists

@Suppress("LoggingSimilarMessage")
class ProjectTasksBuilder(private val context: ProjectContext, private val model: Model) {

    fun build(): TaskGraph {
        // always process in fixed order, no other requirements yet
        val sortedByPath = model.modules.sortedBy { (it.source as PotatoModuleFileSource).buildFile }

        val tasks = TaskGraphBuilder()
        val executeOnChangedInputs = ExecuteOnChangedInputs(context.buildOutputRoot)

        for (module in sortedByPath) {
            val modulePlatforms = module.targetLeafPlatforms

            for (platform in modulePlatforms) {
                for (isTest in listOf(false, true)) {
                    val fragments = module.fragments.filter { it.isTest == isTest && it.platforms.contains(platform) }
                    if (isTest && fragments.all { !it.src.exists() }) {
                        // no test code, assume no code generation
                        // other modules could not depend on this module's tests, so it's ok
                        continue
                    }

                    val prepareDebugAndroidBuildName = if (platform == Platform.ANDROID) {
                        val testSuffix = if (isTest) "Test" else ""
                        val prepareDebugAndroidBuild = TaskName.fromHierarchy(listOf(module.userReadableName, "prepare${testSuffix}DebugAndroidBuild"))
                        tasks.registerTask(AndroidPrepareTask(module, prepareDebugAndroidBuild))
                        prepareDebugAndroidBuild
                    } else null

                    val androidPlatformJarTaskName = if (platform == Platform.ANDROID) {
                        setupAndroidPlatformTask(module, tasks)
                    } else null

                    fun createCompileTask() {
                        val compileTaskName = getTaskName(module, CommonTaskType.COMPILE, platform, isTest = isTest)
                        when (val top = platform.topmostParentNoCommon) {
                            Platform.JVM -> JvmCompileTask(
                                module = module,
                                fragments = fragments,
                                userCacheRoot = context.userCacheRoot,
                                taskOutputRoot = getTaskOutputPath(compileTaskName),
                                taskName = compileTaskName,
                                executeOnChangedInputs = executeOnChangedInputs,
                                projectRoot =  context.projectRoot,
                            )

                            Platform.NATIVE -> NativeCompileTask(
                                module = module,
                                platform = platform,
                                userCacheRoot = context.userCacheRoot,
                                taskOutputRoot = getTaskOutputPath(compileTaskName),
                                executeOnChangedInputs = executeOnChangedInputs,
                                taskName = compileTaskName,
                                tempRoot = context.projectTempRoot,
                                isTest = isTest,
                            )

                            else -> {
                                logger.warn("$top is not supported yet")
                                null
                            }
                        }?.also { task ->
                            tasks.registerTask(
                                task,
                                dependsOn = getTaskName(module, CommonTaskType.DEPENDENCIES, platform, isTest = isTest)
                            )
                        }
                    }

                    fun createResolveTask(): Task =
                        ResolveExternalDependenciesTask(
                            module,
                            context.userCacheRoot,
                            executeOnChangedInputs,
                            isTest = isTest,
                            platform = platform,
                            taskName = getTaskName(module, CommonTaskType.DEPENDENCIES, platform, isTest = isTest)
                        ).also { resolveTask ->
                            tasks.registerTask(resolveTask, dependsOn = buildList {
                                if (platform.topmostParentNoCommon == Platform.ANDROID) {
                                    androidPlatformJarTaskName?.let { add(it) }
                                    prepareDebugAndroidBuildName?.let { add(it) }
                                }
                            })
                        }

                    fun createRunTask() {
                        require(!isTest)
                        require(!module.type.isLibrary())

                        val runTaskName = getTaskName(module, CommonTaskType.RUN, platform, isTest = false)
                        when (val top = platform.topmostParentNoCommon) {
                            Platform.JVM -> JvmRunTask(
                                module = module,
                                userCacheRoot = context.userCacheRoot,
                                projectRoot = context.projectRoot,
                                taskName = runTaskName,
                            )

                            Platform.NATIVE -> NativeRunTask(
                                module = module,
                                projectRoot = context.projectRoot,
                                taskName = runTaskName,
                                platform = platform,
                            )

                            else -> {
                                logger.warn("$top is not supported yet")
                                null
                            }
                        }?.also {
                            tasks.registerTask(
                                it,
                                dependsOn = getTaskName(module, CommonTaskType.COMPILE, platform, isTest = false)
                            )
                        }
                    }

                    fun createTestTask(): Task {
                        require(isTest)

                        val testTaskName = getTaskName(module, CommonTaskType.TEST, platform, isTest = true)
                        return when (val top = platform.topmostParentNoCommon) {
                            Platform.JVM -> JvmTestTask(
                                module = module,
                                userCacheRoot = context.userCacheRoot,
                                projectRoot = context.projectRoot,
                                taskName = testTaskName,
                                taskOutputRoot = getTaskOutputPath(testTaskName),
                            )

                            Platform.NATIVE -> NativeTestTask(
                                module = module,
                                projectRoot = context.projectRoot,
                                taskName = testTaskName,
                            )

                            else -> error("$top is not supported yet")
                        }.also {
                            tasks.registerTask(
                                it,
                                dependsOn = listOf(
                                    getTaskName(
                                        module,
                                        CommonTaskType.COMPILE,
                                        platform,
                                        isTest = true
                                    )
                                )
                            )
                        }
                    }

                    createResolveTask()
                    createCompileTask()

                    // TODO this does not support runtime dependencies
                    //  and dependency graph for it will be different
                    if (isTest) {
                        createTestTask()
                    } else {
                        if (!module.type.isLibrary()) {
                            createRunTask()
                        }
                    }

                    // TODO In the future this code should be near compile task
                    // TODO What to do with fragment.fragmentDependencies?
                    //  I'm not sure, it's just test -> non-test dependency? Otherwise we build it by platforms
                    if (isTest) {
                        tasks.registerDependency(
                            taskName = getTaskName(module, CommonTaskType.COMPILE, platform, true),
                            dependsOn = getTaskName(module, CommonTaskType.COMPILE, platform, false),
                        )
                    }

                    for ((fragment, dependency) in fragments.flatMap { fragment -> fragment.externalDependencies.map { fragment to it } }) {
                        when (dependency) {
                            is MavenDependency -> Unit
                            is PotatoModuleDependency -> {
                                // runtime dependencies are not required to be in compile tasks graph
                                if (dependency.compile) {
                                    // TODO test with non-resolved dependency on module
                                    val resolvedDependencyModule = dependency.resolveModuleDependency()

                                    tasks.registerDependency(
                                        taskName = getTaskName(
                                            module,
                                            CommonTaskType.COMPILE,
                                            platform,
                                            isTest = isTest
                                        ),
                                        dependsOn = getTaskName(
                                            resolvedDependencyModule,
                                            CommonTaskType.COMPILE,
                                            platform,
                                            isTest = false
                                        ),
                                    )
                                }
                            }

                            else -> error(
                                "Unsupported dependency type: '$dependency' " +
                                        "at module '${module.source}' fragment '${fragment.name}'"
                            )
                        }
                    }
                }
            }
        }

        return tasks.build()
    }

    private fun setupAndroidPlatformTask(module: PotatoModule, tasks: TaskGraphBuilder): TaskName? {
        return module
            .fragments
            .filter { Platform.ANDROID in it.platforms }
            .firstOrNull { !it.isTest }
            ?.let { androidFragment ->
                androidFragment.parts.find<AndroidPart>()?.targetSdk?.let { targetSdk ->
                    val androidCompileTaskName = TaskName.fromHierarchy(listOf(module.userReadableName, "downloadAndroidSdk"))
                    tasks.registerTask(GetAndroidPlatformJarTask("android-$targetSdk", androidCompileTaskName))
                    androidCompileTaskName
                }
            }
    }

    private fun PotatoModuleDependency.resolveModuleDependency(): PotatoModule = with(CliProblemReporterContext()) {
        val result = model.module.get()

        if (problemReporter.wereProblemsReported()) {
            error("failed to build tasks graph, refer to the errors above")
        }

        result
    }

    // All task binding between themselves happens here, so let's keep it private here
    // This function is the only place where we define task naming convention
    private fun getTaskName(module: PotatoModule, type: CommonTaskType, platform: Platform, isTest: Boolean): TaskName {
        val testSuffix = if (isTest) "Test" else ""
        val platformSuffix = platform.pretty.replaceFirstChar { it.uppercase() }

        val taskName = when (type) {
            CommonTaskType.COMPILE -> "compile$platformSuffix$testSuffix"
            CommonTaskType.DEPENDENCIES -> "resolveDependencies$platformSuffix$testSuffix"
            CommonTaskType.RUN -> {
                require(!isTest)
                "run$platformSuffix"
            }

            CommonTaskType.TEST -> {
                require(isTest)
                "test$platformSuffix"
            }
        }

        return TaskName.fromHierarchy(listOf(module.userReadableName, taskName))
    }

    // Keep it private, code in any other class should not get direct access to the output path of a particular task
    // Let's assume every task may get another task output only from its dependencies
    // private getTaskOutputPath helps to enforce that in a gentle way
    private fun getTaskOutputPath(taskName: TaskName): TaskOutputRoot {
        val out = context.buildOutputRoot.path.resolve("tasks").resolve(taskName.name.replace(':', '_'))
        return TaskOutputRoot(path = out)
    }

    // All task binding between themselves happens here, so let's keep it private
    private enum class CommonTaskType {
        COMPILE,
        DEPENDENCIES,
        RUN,
        TEST,
    }

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
}