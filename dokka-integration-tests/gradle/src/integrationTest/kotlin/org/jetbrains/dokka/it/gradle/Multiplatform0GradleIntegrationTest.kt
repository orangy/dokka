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

class Multiplatform0GradleIntegrationTest : AbstractGradleIntegrationTest() {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-multiplatform-0")
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }
        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        // `enableGranularSourceSetsMetadata` and  `enableDependencyPropagation` flags are enabled by default since 1.6.20.
        // remove when this test is executed with Kotlin >= 1.6.20
        val result = if (buildVersions.kotlinVersion < "1.6.20")
            createGradleRunner(
                buildVersions,
                "dokkaHtml",
                "-s",
                "-Pkotlin.mpp.enableGranularSourceSetsMetadata=true",
                "-Pkotlin.native.enableDependencyPropagation=false"
            ).buildRelaxed()
        else
            createGradleRunner(buildVersions, "dokkaHtml", "-s").buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtml")).outcome)

        val dokkaOutputDir = File(projectDir, "build/dokka/html")
        assertTrue(dokkaOutputDir.isDirectory, "Missing dokka output directory")

        dokkaOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
