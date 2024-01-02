/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.schemaConverter

import org.jetbrains.amper.core.messages.ProblemReporterContext
import org.jetbrains.amper.frontend.assertNodeType
import org.jetbrains.amper.frontend.schema.AndroidSettings
import org.jetbrains.amper.frontend.schema.ComposeSettings
import org.jetbrains.amper.frontend.schema.IosFrameworkSettings
import org.jetbrains.amper.frontend.schema.IosSettings
import org.jetbrains.amper.frontend.schema.JavaSettings
import org.jetbrains.amper.frontend.schema.JvmSettings
import org.jetbrains.amper.frontend.schema.KotlinSettings
import org.jetbrains.amper.frontend.schema.KoverHtmlSettings
import org.jetbrains.amper.frontend.schema.KoverSettings
import org.jetbrains.amper.frontend.schema.KoverXmlSettings
import org.jetbrains.amper.frontend.schema.PublishingSettings
import org.jetbrains.amper.frontend.schema.SerializationSettings
import org.jetbrains.amper.frontend.schema.Settings
import org.jetbrains.amper.frontend.schemaConverter.psi.adjustTrace
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode

context(ProblemReporterContext)
internal fun Node.convertSettings() = assertNodeType<MappingNode, Settings>("settings") {
    doConvertSettings()
}?.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.doConvertSettings() = Settings().apply {
    // TODO Report wrong node types.
    java(tryGetChildNode("java")?.asMappingNode()?.convertJavaSettings())
    jvm(tryGetChildNode("jvm")?.asMappingNode()?.convertJvmSettings())
    android(tryGetChildNode("android")?.asMappingNode()?.convertAndroidSettings())
    kotlin(tryGetChildNode("kotlin")?.asMappingNode()?.convertKotlinSettings())
    compose(tryGetChildNode("compose")?.convertComposeSettings())
    ios(tryGetMappingNode("ios")?.convertIosSettings())
    publishing(tryGetMappingNode("publishing")?.convertPublishingSettings())
    kover(tryGetMappingNode("kover")?.convertKoverSettings())
}

context(ProblemReporterContext)
internal fun MappingNode.convertJavaSettings() = JavaSettings().apply {
    source(tryGetScalarNode("source"))
}.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.convertJvmSettings() = JvmSettings().apply {
    target(tryGetScalarNode("target"))
    mainClass(tryGetScalarNode("mainClass"))
}.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.convertAndroidSettings() = AndroidSettings().apply {
    compileSdk(tryGetScalarNode("compileSdk"))
    minSdk(tryGetScalarNode("minSdk"))
    maxSdk(tryGetScalarNode("maxSdk"))
    targetSdk(tryGetScalarNode("targetSdk"))
    applicationId(tryGetScalarNode("applicationId"))
    namespace(tryGetScalarNode("namespace"))
}.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.convertKotlinSettings() = KotlinSettings().apply {
    // TODO Report wrong types.
    languageVersion(tryGetScalarNode("languageVersion"))
    apiVersion(tryGetScalarNode("apiVersion"))

    allWarningsAsErrors(tryGetScalarNode("allWarningsAsErrors"))
    suppressWarnings(tryGetScalarNode("suppressWarnings"))
    verbose(tryGetScalarNode("verbose"))
    debug(tryGetScalarNode("debug"))
    progressiveMode(tryGetScalarNode("progressiveMode"))

    freeCompilerArgs(tryGetScalarSequenceNode("freeCompilerArgs"))
    linkerOpts(tryGetScalarSequenceNode("linkerOpts"))
    languageFeatures(tryGetScalarSequenceNode("languageFeatures"))
    optIns(tryGetScalarSequenceNode("optIns"))

    serialization(tryGetChildNode("serialization")?.convertSerializationSettings())
}.adjustTrace(this)

context(ProblemReporterContext)
internal fun Node.convertSerializationSettings() = when (this) {
    is ScalarNode -> SerializationSettings().apply { engine(this@convertSerializationSettings) }
    is MappingNode -> SerializationSettings().apply { engine(tryGetScalarNode("engine")) }
    else -> null
}?.adjustTrace(this)

context(ProblemReporterContext)
internal fun Node.convertComposeSettings() = when (this) {
    // TODO Report wrong value.
    is ScalarNode -> ComposeSettings().apply { enabled(value == "enabled").adjustTrace(this@convertComposeSettings) }
    is MappingNode -> ComposeSettings().apply { enabled(tryGetScalarNode("enabled")) }
    else -> null
}?.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.convertIosSettings() = IosSettings().apply {
    teamId(tryGetScalarNode("teamId"))
    framework(tryGetMappingNode("framework")?.convertIosFrameworkSettings())
}.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.convertIosFrameworkSettings() = IosFrameworkSettings().apply {
    basename(tryGetScalarNode("basename"))
    isStatic(tryGetScalarNode("isStatic"))
    // TODO Report wrong types/values.
    mappings(convertScalarKeyedMap { key -> asScalarNode()?.value?.takeIf { key != "basename" && key != "isStatic" } })
}.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.convertPublishingSettings() = PublishingSettings().apply {
    group(tryGetScalarNode("group"))
    version(tryGetScalarNode("version"))
}.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.convertKoverSettings() = KoverSettings().apply {
    enabled(tryGetScalarNode("enabled"))
    xml(tryGetMappingNode("xml")?.convertKoverXmlSettings())
    html(tryGetMappingNode("html")?.convertKoverHtmlSettings())
}.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.convertKoverXmlSettings() = KoverXmlSettings().apply {
    onCheck(tryGetScalarNode("onCheck"))
    reportFile(tryGetScalarNode("reportFile"))
}.adjustTrace(this)

context(ProblemReporterContext)
internal fun MappingNode.convertKoverHtmlSettings() = KoverHtmlSettings().apply {
    onCheck(tryGetScalarNode("onCheck"))
    title(tryGetScalarNode("title"))
    charset(tryGetScalarNode("charset"))
    reportDir(tryGetScalarNode("reportDir"))
}.adjustTrace(this)