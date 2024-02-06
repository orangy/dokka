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
import kotlin.test.assertTrue

class BasicCachingIntegrationTest : AbstractGradleCachingIntegrationTest() {


    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        setupProject(buildVersions, projectDir)

        runAndAssertOutcomeAndContents(buildVersions, TaskOutcome.SUCCESS)
        runAndAssertOutcomeAndContents(buildVersions, TaskOutcome.FROM_CACHE)
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun localDirectoryPointingToRoot(buildVersions: BuildVersions) {
        setupProject(buildVersions, projectDir)

        fun String.findAndReplace(oldValue: String, newValue: String): String {
            assertTrue(oldValue in this, "Expected to replace '$oldValue'")
            return replace(oldValue, newValue)
        }
        val projectKts = projectDir.resolve("build.gradle.kts")

        projectKts.readText()
            .findAndReplace("localDirectory.set(file(\"src/main\"))", "localDirectory.set(projectDir)")
            .findAndReplace("integration-tests/gradle/projects/it-basic/src/main", "integration-tests/gradle/projects/it-basic")
            .also { projectKts.writeText(it) }

        runAndAssertOutcomeAndContents(buildVersions, TaskOutcome.SUCCESS)
        projectDir.resolve("unrelated.txt").writeText("modified")
        // despite projectDir is used as an input in localDirectory, changing its contents shouldn't invalidate the cache
        runAndAssertOutcomeAndContents(buildVersions, TaskOutcome.FROM_CACHE)

        projectKts.readText()
            .findAndReplace("localDirectory.set(projectDir)", "localDirectory.set(file(\"src\"))")
            .also { projectKts.writeText(it) }
        // changing localDirectory path invalidates cached task results
        runAndAssertOutcome(buildVersions, TaskOutcome.SUCCESS)
    }


    private fun runAndAssertOutcomeAndContents(buildVersions: BuildVersions, expectedOutcome: TaskOutcome) {
        runAndAssertOutcome(buildVersions, expectedOutcome)
        File(projectDir, "build/dokka/html").assertHtmlOutputDir()
    }

    private fun runAndAssertOutcome(buildVersions: BuildVersions, expectedOutcome: TaskOutcome) {
        val result = createGradleRunner(
            buildVersions,
            "clean",
            "dokkaHtml",
            "-s",
            "-Dorg.gradle.caching.debug=true",
            "--build-cache"
        ).buildRelaxed()

        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaHtml")).outcome)
    }
}
