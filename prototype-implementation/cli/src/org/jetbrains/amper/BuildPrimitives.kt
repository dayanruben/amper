/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper

import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.amper.processes.ProcessResult
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.jetbrains.amper.diagnostics.spanBuilder
import org.jetbrains.amper.diagnostics.useWithScope
import org.jetbrains.amper.processes.awaitAndGetAllOutput
import org.jetbrains.amper.processes.withGuaranteedTermination
import org.jetbrains.amper.util.ShellQuoting
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.deleteIfExists
import kotlin.io.path.pathString

/**
 * Ordinary operations like running processes and copying files require
 * a comprehensive support in a build system to make it observable
 */
object BuildPrimitives {
    /**
     * Delete one file on background, report errors if any to log warning only
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun deleteLater(file: Path) {
        // just in case global scope will be cancelled
        file.toFile().deleteOnExit()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                file.deleteIfExists()
            } catch (t: Throwable) {
                logger.warn("Unable to delete file '$file': ${t.message}", t)
            }
        }
    }

    /**
     * Starts a new process with the given [command] in [workingDir], and awaits the result.
     * While waiting, stdout and stderr are printed to the console, but they are also entirely collected in memory as
     * part of the returned [ProcessResult].
     *
     * It is not supported to write to the standard input of the started process.
     *
     * If this function is cancelled before the process has terminated, it kills the process (first normally then
     * forcibly), and cleans the stream readers. If the process is not killable and hangs, this function will also hang
     * instead of returning (otherwise the zombie process could leak).
     *
     * **Note:** since the blocking reads of standard streams are not cancellable, this function may have to wait for
     * the read of the current line to complete before returning. This is to ensure no coroutines are leaked.
     * This wait should be reasonable anyway because the process is killed on cancellation, so no more output should be
     * written in that case.
     */
    // TODO sometimes capturing the entire stdout/stderr in memory won't work
    //  do we want to offload big (and probably only big outputs) to the disk?
    suspend fun runProcessAndGetOutput(command: List<String>, workingDir: Path, environment: Map<String, String> = emptyMap()): ProcessResult =
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(command)
                .directory(workingDir.toFile())
                .also { it.environment().putAll(environment) }
                .start()

            process.withGuaranteedTermination {
                try {
                    process.outputStream.close()
                } catch (t: IOException) {
                    // we are not interested whether this operation fails
                    logger.warn("Unable to close process stdin: ${t.message}", t)
                }
                process.awaitAndGetAllOutput(
                    onStdoutLine = System.out::println,
                    onStderrLine = System.err::println,
                )
            }
        }

    /**
     * Starts a new process with the given [command] in [workingDir], and awaits the result.
     * While waiting, stdout and stderr are printed to the console, but they are also entirely collected in memory as
     * part of the result. The result (exit code, stdout, stderr) is recorded in the provided [span].
     *
     * It is not supported to write to the standard input of the started process.
     *
     * If this function is cancelled before the process has terminated, it kills the process (first normally then
     * forcibly), and cleans the stream readers. If the process is not killable and hangs, this function will also hang
     * instead of returning (otherwise the zombie process could leak).
     *
     * **Note:** since the blocking reads of standard streams are not cancellable, this function may have to wait for
     * the read of the current line to complete before returning. This is to ensure no threads are leaked.
     * This wait should be reasonable anyway because the process is killed, so no more output should be written.
     */
    suspend fun runProcessAndAssertExitCode(command: List<String>, workingDir: Path, span: Span, environment: Map<String, String> = emptyMap()) {
        require(command.isNotEmpty())

        logger.info("Calling ${ShellQuoting.quoteArgumentsPosixShellWay(command)}")

        val result = runProcessAndGetOutput(command, workingDir, environment)
        val stdout = result.stdout
        val stderr = result.stderr

        span.setAttribute("exit-code", result.exitCode.toLong())
        span.setAttribute("stdout", stdout)
        span.setAttribute("stderr", stderr)

        if (result.exitCode != 0) {
            error(
                "${command.first()} exited with exit code ${result.exitCode}" +
                        (if (stderr.isNotEmpty()) "\nSTDERR:\n${stderr}\n" else "") +
                        (if (stdout.isNotEmpty()) "\nSTDOUT:\n${stdout}\n" else "")
            )
        }
    }

    // defaults are selected for build system-stuff
    @OptIn(ExperimentalPathApi::class)
    suspend fun copy(from: Path, to: Path, overwrite: Boolean = false, followLinks: Boolean = true) {
        // Do not change coroutine context, we want to stay in tasks pool

        spanBuilder("copy")
            .setAttribute("from", from.pathString)
            .setAttribute("to", to.pathString)
            .useWithScope {
                // TODO is it really interruptible here?
                runInterruptible {
                    from.copyToRecursively(to, overwrite = overwrite, followLinks = followLinks)
                }
            }
    }

    private val logger = LoggerFactory.getLogger(javaClass)
}
