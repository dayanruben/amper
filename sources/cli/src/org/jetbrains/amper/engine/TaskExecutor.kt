/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.jetbrains.amper.diagnostics.spanBuilder
import org.jetbrains.amper.diagnostics.useWithScope
import org.jetbrains.amper.tasks.TaskResult
import org.slf4j.MDC

class TaskExecutor(
    private val graph: TaskGraph,
    private val mode: Mode,
    private val progressListener: TaskProgressListener = TaskProgressListener.Noop,
) {
    init {
        // verify all dependencies are resolved
        for ((taskName, dependsOn) in graph.dependencies) {
            if (!graph.nameToTask.containsKey(taskName)) {
                error("Task '$taskName' does not exist, yet it defines dependencies")
            }
            for (dependency in dependsOn) {
                if (!graph.nameToTask.containsKey(dependency)) {
                    error("Task '$taskName' depends on task '$dependency' which does not exist")
                }
            }
        }

        check(!graph.nameToTask.containsKey(rootTaskName)) {
            "task graph should not contain internal root task name '${rootTaskName.name}'"
        }
    }

    private val availableProcessors = Runtime.getRuntime().availableProcessors()

    @OptIn(ExperimentalCoroutinesApi::class)
    // TODO Should be configurable
    private val tasksDispatcher = Dispatchers.IO.limitedParallelism(availableProcessors.coerceAtLeast(3))

    // Dispatch on default dispatcher, execute on tasks dispatcher
    // Task failures do not throw, instead all exceptions are returned as a map
    suspend fun run(tasksToRun: Set<TaskName>): Map<TaskName, Result<TaskResult>> = withContext(Dispatchers.Default) {
        val results = mutableMapOf<TaskName, Deferred<Result<TaskResult>>>()
        try {
            coroutineScope {
                runTask(rootTaskName, emptyList(), results, rootTaskDependencies = tasksToRun)
                results.mapValues { it.value.await() }
            }
        } catch (e: CancellationException) {
            results.mapValues { if (it.value.isCancelled) Result.failure(e) else it.value.await() }
        }
    }

    // TODO we need to re-evaluate task order execution later
    private suspend fun runTask(
        taskName: TaskName,
        currentPath: List<TaskName>,
        taskResults: MutableMap<TaskName, Deferred<Result<TaskResult>>>,
        rootTaskDependencies: Set<TaskName>,
    ): Result<TaskResult> = withContext(Dispatchers.Default) {
        // TODO slow, we can do better for sure
        if (currentPath.contains(taskName)) {
            error("Found a cycle in task execution graph:\n" +
                    (currentPath + taskName).joinToString(" -> ") { it.name })
        }
        val newPath = currentPath + taskName

        fun taskDependencies(taskName: TaskName): Collection<TaskName> = when (taskName) {
            rootTaskName -> rootTaskDependencies
            else -> graph.dependencies[taskName] ?: emptySet()
        }

        suspend fun runTask(taskName: TaskName, dependenciesResult: List<TaskResult>): TaskResult = when (taskName) {
            rootTaskName -> object : TaskResult {
                override val dependencies: List<TaskResult> = dependenciesResult
            }

            else -> {
                val task = graph.nameToTask[taskName] ?: error("Unable to find task by name: ${taskName.name}")
                progressListener.taskStarted(taskName).use {
                    MDC.put("amper-task-name", taskName.name)
                    withContext(MDCContext()) {
                        task.run(dependenciesResult)
                    }
                }
            }
        }

        coroutineScope {
            val results = taskDependencies(taskName)
                .map { dependsOn ->
                    dependsOn to synchronized(taskResults) {
                        val existingResult = taskResults[dependsOn]
                        if (existingResult != null) {
                            existingResult
                        } else {
                            val newDeferred = async {
                                runTask(dependsOn, newPath, taskResults, rootTaskDependencies)
                            }
                            taskResults[dependsOn] = newDeferred
                            newDeferred
                        }
                    }
                }
                .map { (dependsOn, deferredResult) -> dependsOn to deferredResult.await() }
                .map { (dependsOn, result) ->
                    result.getOrElse { ex ->
                        // terminate task execution since dependency failed
                        return@coroutineScope Result.failure(CancellationException("task dependency '$dependsOn' failed", ex))
                    }
                }

            withContext(tasksDispatcher) {
                spanBuilder("task ${taskName.name}")
                    .useWithScope {
                        val result = runCatching {
                            runTask(taskName, results)
                        }

                        when (mode) {
                            Mode.GREEDY -> result
                            Mode.FAIL_FAST -> if (result.isFailure) {
                                throw TaskExecutionFailed(taskName, result.exceptionOrNull()!!)
                            } else result
                        }
                    }
            }
        }
    }

    enum class Mode {
        /**
         * Upon task failure continue execution of all other tasks,
         * that are independent of the failed task in the task graph
         */
        GREEDY,

        /**
         * Fail on a first failed task, cancel all running and queued tasks upon failure
         */
        FAIL_FAST,
    }

    class TaskExecutionFailed(val taskName: TaskName, val exception: Throwable)
        : Exception("Task '${taskName.name}' failed: ${exception.message}", exception)

    companion object {
        private val rootTaskName = TaskName(":")
    }
}
