/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.*

class MultiModule0IntegrationTest : AbstractGradleIntegrationTest() {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-multimodule-0")
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }
        File(templateProjectDir, "moduleA").copyRecursively(File(projectDir, "moduleA"))
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":moduleA:dokkaHtmlMultiModule",
            ":moduleA:dokkaGfmMultiModule",
            ":moduleA:dokkaJekyllMultiModule",
            "-s",
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaHtmlMultiModule")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaGfmMultiModule")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaJekyllMultiModule")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleB:dokkaHtmlPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleC:dokkaHtmlPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleB:dokkaGfmPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleC:dokkaGfmPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleB:dokkaJekyllPartial")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:moduleC:dokkaJekyllPartial")).outcome)


        val outputDir = File(projectDir, "moduleA/build/dokka/htmlMultiModule")
        assertTrue(outputDir.isDirectory, "Missing dokka output directory")

        assertTrue(
            outputDir.allHtmlFiles().any(),
            "Expected at least one html file being generated"
        )

        outputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
            assertNoUnsubstitutedTemplatesInHtml(file)
        }

        val modulesFile = File(outputDir, "index.html")
        assertTrue(modulesFile.isFile, "Missing index.html file")

        val modulesFileText = modulesFile.readText()
        assertTrue(
            "moduleB" in modulesFileText,
            "Expected moduleB being mentioned in -modules.html"
        )
        assertTrue(
            "moduleC" in modulesFileText,
            "Expected moduleC being mentioned in -modules.html"
        )

        val htmlsWithHomepageLink = outputDir.walkTopDown().filter {
            it.isFile && it.extension == "html" && it.name != "navigation.html"
        }.toList()

        assertEquals(16, htmlsWithHomepageLink.size)

        htmlsWithHomepageLink.forEach {
            assertTrue(
                it.readText().contains(
                    """https://github.com/Kotlin/dokka/tree/master/dokka-integration-tests/gradle/projects/it-multimodule-0/"""
                ),
                "File ${it.absolutePath} doesn't contain link to homepage"
            )
        }

        val gfmOutputDir = File(projectDir, "moduleA/build/dokka/gfmMultiModule")
        assertTrue(gfmOutputDir.isDirectory, "Missing dokka GFM output directory")

        assertTrue(
            gfmOutputDir.allGfmFiles().any(),
            "Expected at least one md file being generated"
        )

        gfmOutputDir.allGfmFiles().forEach { file ->
            assertFalse("GfmCommand" in file.readText())
        }
    }
}
