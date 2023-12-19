/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.frontend.helper

import org.jetbrains.amper.frontend.ReaderCtx
import org.jetbrains.amper.frontend.aomBuilder.buildAom
import org.jetbrains.amper.frontend.old.helper.BuildFileAware
import org.jetbrains.amper.frontend.old.helper.TestWithBuildFile
import org.jetbrains.amper.frontend.processing.readTemplatesAndMerge
import org.jetbrains.amper.frontend.processing.replaceCatalogDependencies
import org.jetbrains.amper.frontend.processing.validateSchema
import org.jetbrains.amper.frontend.schemaConverter.ConvertCtx
import org.jetbrains.amper.frontend.schemaConverter.convertModule
import java.io.File
import java.io.StringReader
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.reader


context(TestWithBuildFile)
fun diagnosticsTest(caseName: String) = DiagnosticsTestRun(caseName).doTest()

class DiagnosticsTestRun(caseName: String) : BaseTestRun(caseName) {

    context(BuildFileAware, TestProblemReporterContext)
    override fun getInputContent(inputPath: Path): String {
        // Fix paths, so they will point to resources.
        val processPath = Path(".").absolute().normalize()
        val testResourcesPath = processPath / "testResources"
        val ctx = ReaderCtx {
            if (it.startsWith(testResourcesPath)) it.reader()
            else {
                val relative = processPath.relativize(it)
                val resolved = testResourcesPath.resolve(relative)
                resolved.takeIf { resolved.exists() }?.reader()
            }
        }

        // Read module.
        val cleared = inputPath.readText().removeDiagnosticsAnnotations()
        val schemaModule = with(ctx) {
            with(ConvertCtx(inputPath.parent)) {
                convertModule { StringReader(cleared) }
                    .readTemplatesAndMerge()
                    .replaceCatalogDependencies()
                    .validateSchema()
            }
        }

        // Build AOM if no fatals.
        if (!problemReporter.hasFatal) {
            mapOf(inputPath to schemaModule).buildAom().first()
        }

        // Collect errors.
        val errors = problemReporter.getErrors()
        return annotateTextWithDiagnostics(cleared, errors) {
            it.replace(buildDir.absolutePathString() + File.separator, "")
        }.trimTrailingWhitespacesAndEmptyLines()
    }

    context(BuildFileAware, TestProblemReporterContext)
    override fun getExpectContent(inputPath: Path, expectedPath: Path) =
        readContentsAndReplace(inputPath).trimTrailingWhitespacesAndEmptyLines()
}