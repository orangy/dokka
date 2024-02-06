/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GradleRelocatedCachingIntegrationTest : AbstractGradleCachingIntegrationTest() {

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        setupProject(buildVersions, projectFolder(1))
        setupProject(buildVersions, projectFolder(2))

        runAndAssertOutcomeAndContents(buildVersions, projectFolder(1), TaskOutcome.SUCCESS)
        runAndAssertOutcomeAndContents(buildVersions, projectFolder(2), TaskOutcome.FROM_CACHE)
    }

    private fun runAndAssertOutcomeAndContents(buildVersions: BuildVersions, project: File, expectedOutcome: TaskOutcome) {
        val result = createGradleRunner(
            buildVersions,
            "clean",
            "dokkaHtml",
            "-s",
            "-Dorg.gradle.caching.debug=true",
            "--build-cache"
        ).withProjectDir(project).buildRelaxed()

        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaHtml")).outcome)

        File(project, "build/dokka/html").assertHtmlOutputDir()
    }

    private fun projectFolder(index: Int) = File(projectDir.absolutePath + index)
}
