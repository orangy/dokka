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
 * Tests for Dokka's configuration options of the Gradle runner.
 *
 * Options can be checked to work in combination with each other:
 * for instance, you can check that `reportUndocumented` and `failOnWarning`
 * work in synergy when both set to true.
 *
 * Configuration options can be passed as project properties using Gradle CLI arguments.
 * For example, passing `-Pname=value` to Gradle will create a project-wide property with
 * key `name` and value `value`, which you can use to set the corresponding option's value
 * using Dokka's configuration DSL.
 */
class ConfigurationTest : AbstractGradleIntegrationTest() {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-configuration")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    /**
     * The test project contains some undocumented declarations, so if both `reportUndocumented`
     * and `failOnWarning` are enabled - it should fail
     */
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(LatestTestedVersionsArgumentsProvider::class)
    @Suppress("FunctionName")
    fun `should fail with DokkaException and readable message if failOnWarning is triggered`(
        buildVersions: BuildVersions
    ) {
        val result = createGradleRunner(
            buildVersions,
//            "--info",
            "--stacktrace",
            "-Preport_undocumented=true",
            "-Pfail_on_warning=true",
            "dokkaHtml"
        ).buildAndFail()

        assertEquals(TaskOutcome.FAILED, assertNotNull(result.task(":dokkaHtml")).outcome)

        result.output.contains("> Task :dokkaHtml FAILED")
        result.output.contains(
            """
               FAILURE: Build failed with an exception\\.
               
               \* What went wrong:
               Execution failed for task ':dokkaHtml'\\.
               > Failed with warningCount=\d and errorCount=\d
           """.trimIndent().toRegex()
        )

        result.output.contains(
            "Caused by: org\\.jetbrains\\.dokka\\.DokkaException: Failed with warningCount=\\d and errorCount=\\d".toRegex()
        )
    }
}
