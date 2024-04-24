/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.amper.engine

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.jetbrains.amper.cli.TaskGraphBuilder
import org.jetbrains.amper.tasks.TaskResult
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

class TaskExecutorTest {
    @Test
    fun diamondTaskDependencies() {
        val builder = TaskGraphBuilder()
        builder.registerTask(TestTask("D"))
        builder.registerTask(TestTask("B"), listOf(TaskName("D")))
        builder.registerTask(TestTask("C"), listOf(TaskName("D")))
        builder.registerTask(TestTask("A"), listOf(TaskName("B"), TaskName("C")))
        val graph = builder.build()
        val executor = TaskExecutor(graph, TaskExecutor.Mode.GREEDY)
        runBlocking {
            executor.run(setOf(TaskName("A")))
        }
        if (executed != listOf("D", "B", "C", "A") && executed != listOf("D", "C", "B", "A")) {
            fail("Wrong execution order: $executed")
        }
    }

    @Test
    fun executionExecutesAllPossibleTasksOnTaskFailureInGreedyMode() {
        // Given the task graph dependencies:
        // D -> C
        // D -> B
        // B -> A
        // if A fails, C should be still executed

        val builder = TaskGraphBuilder()
        builder.registerTask(TestTask("A", throwException = true))
        builder.registerTask(TestTask("B"), listOf(TaskName("A")))
        builder.registerTask(TestTask("C", delayMs = 500)) // add enough time for A to cancel execution of itself
        builder.registerTask(TestTask("D"), listOf(TaskName("B"), TaskName("C")))
        val graph = builder.build()
        val executor = TaskExecutor(graph, TaskExecutor.Mode.GREEDY)
        val result = runBlocking {
            executor.run(setOf(TaskName("D")))
        }
        assertEquals("C", (result.getValue(TaskName("C")).getOrThrow() as TestTaskResult).taskName.name)
        assertTrue(result.getValue(TaskName("A")).exceptionOrNull() is IllegalStateException)
        assertTrue(result.getValue(TaskName("B")).exceptionOrNull() is CancellationException)
        assertTrue(result.getValue(TaskName("D")).exceptionOrNull() is CancellationException)
    }

    @Test
    fun executionTerminatesOnTaskFailureInFailFastMode() {
        // Given the task graph dependencies:
        // D -> C
        // D -> B
        // B -> A
        // if A fails, C should not be still executed because of FAIL_FAST mode

        val builder = TaskGraphBuilder()
        builder.registerTask(TestTask("A", throwException = true))
        builder.registerTask(TestTask("B"), listOf(TaskName("A")))
        builder.registerTask(TestTask("C", delayMs = 500)) // add enough time for A to cancel execution of itself
        builder.registerTask(TestTask("D"), listOf(TaskName("B"), TaskName("C")))
        val graph = builder.build()
        val executor = TaskExecutor(graph, TaskExecutor.Mode.FAIL_FAST)
        val result = assertFailsWith(TaskExecutor.TaskExecutionFailed::class) {
            runBlocking {
                executor.run(setOf(TaskName("D")))
            }
        }
        assertEquals("Task 'A' failed: throw", result.message)
    }

    @Test
    fun rootTasksExecuteInParallel() = runTest {
        val builder = TaskGraphBuilder()
        builder.registerTask(TestTask("A", waitForMaxParallelTasksCount = 3))
        builder.registerTask(TestTask("B", waitForMaxParallelTasksCount = 3))
        builder.registerTask(TestTask("C", waitForMaxParallelTasksCount = 3))
        val graph = builder.build()
        val executor = TaskExecutor(graph, TaskExecutor.Mode.FAIL_FAST)
        executor.run(setOf(TaskName("A"), TaskName("B"), TaskName("C")))
        assertEquals(3, maxParallelTasksCount.value)
    }

    private val executed = mutableListOf<String>()
    private val tasksCount = atomic(0)
    private val maxParallelTasksCount = atomic(0)
    private inner class TestTask(
        val name: String,
        val delayMs: Long = 0,
        val waitForMaxParallelTasksCount: Int? = null,
        val throwException: Boolean = false,
    ): Task {
        override val taskName: TaskName
            get() = TaskName(name)

        override suspend fun run(dependenciesResult: List<TaskResult>): TaskResult {
            val currentTasksCount = tasksCount.incrementAndGet()
            maxParallelTasksCount.update { max -> max(max, currentTasksCount) }
            try {
                synchronized(executed) {
                    executed.add(name)
                }
                if (waitForMaxParallelTasksCount != null) {
                    withTimeout(10.seconds) {
                        while (true) {
                            if (maxParallelTasksCount.value == waitForMaxParallelTasksCount) {
                                break
                            }
                            delay(10)
                        }
                    }
                }
                delay(delayMs)
                if (throwException) error("throw")
                return TestTaskResult(taskName, dependenciesResult)
            } finally {
                tasksCount.decrementAndGet()
            }
        }
    }

    private class TestTaskResult(val taskName: TaskName, override val dependencies: List<TaskResult>) : TaskResult
}
