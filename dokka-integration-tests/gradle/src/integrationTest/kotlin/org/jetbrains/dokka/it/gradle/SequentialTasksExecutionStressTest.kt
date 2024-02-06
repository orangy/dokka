/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 *  Creates 100 tasks for the test project and runs them sequentially under low memory settings.
 *
 *  If the test passes, it's likely there are no noticeable memory leaks.
 *  If it fails, it's likely that memory is leaking somewhere.
 */
class SequentialTasksExecutionStressTest : AbstractGradleIntegrationTest() {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-sequential-tasks-execution-stress")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(LatestTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            "runTasks",
//            "--info",
            "--stacktrace",
            "-Ptask_number=100",
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":runTasks")).outcome)
    }
}
