/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.assertContains
import kotlin.test.assertNotNull


/**
 * Assert that the [BuildResult] has a task in the task graph.
 *
 * @returns the task, if it exists.
 */
fun BuildResult.shouldHaveTask(taskPath: String): BuildTask {
    val actual = task(taskPath)
    assertNotNull(actual) {
        "Could not find task $taskPath in BuildResult. All tasks: ${tasks.map { it.path }}"
    }
    return actual
}

/**
 * Assert that the [outcome][TaskOutcome] of a [BuildTask] is any of [expectedOutcomes].
 */
fun BuildTask.shouldHaveOutcome(
    vararg expectedOutcomes: TaskOutcome
) {
    require(expectedOutcomes.isNotEmpty()) {
        "Invalid assertion. Must have at least 1 expected TaskOutcome, but got none."
    }

    val message = buildString {
        append("Expected that task $path outcome was")

        if (expectedOutcomes.size == 1) {
            append(" ${expectedOutcomes.single()}, ")
        } else {
            append(" any of ${expectedOutcomes.toList()}, ")
        }
        append("but was actually $outcome")
    }

    assertContains(expectedOutcomes.toList(), outcome, message)
}
