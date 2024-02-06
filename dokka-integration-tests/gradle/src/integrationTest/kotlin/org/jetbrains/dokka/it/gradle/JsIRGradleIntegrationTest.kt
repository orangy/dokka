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
import kotlin.test.assertTrue

class JsIRGradleIntegrationTest : AbstractGradleIntegrationTest() {

    private val ignoredKotlinVersions = setOf(
        // There were some breaking refactoring changes in kotlin react wrapper libs in 1.4.0 -> 1.5.0,
        // some core react classes were moved from `react-router-dom` to `react` artifacts.
        // Writing an integration test project that would work for both 1.4.0 and 1.5.0 would involve
        // ugly solutions, so these versions are ignored. Not a big loss given they are deprecated as of this moment.
        "1.4.0", "1.4.32"
    )

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-js-ir-0")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .filterNot { it.name == "local.properties" }
            .filterNot { it.name.startsWith("gradlew") }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @OnlyDescriptors
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        if (ignoredKotlinVersions.contains(buildVersions.kotlinVersion)) {
            return
        }

        val reactVersion = TestedVersions.KT_REACT_WRAPPER_MAPPING[buildVersions.kotlinVersion]
            ?: throw IllegalStateException("Unspecified version of react for kotlin " + buildVersions.kotlinVersion)
        val result = createGradleRunner(buildVersions, "-Preact_version=$reactVersion", "dokkaHtml", "-s").buildRelaxed()
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtml")).outcome)

        val htmlOutputDir = File(projectDir, "build/dokka/html")
        assertTrue(htmlOutputDir.isDirectory, "Missing html output directory")

        assertTrue(
            htmlOutputDir.allHtmlFiles().count() > 0,
            "Expected html files in html output directory"
        )

        htmlOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoUnresolvedLinks(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
