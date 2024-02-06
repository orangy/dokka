/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.*

/**
 * The test aims to check links to declarations from a package that is in two different modules.
 *
 * E.g.
 *
 * The class `com.example.classA` is in `moduleA`
 * and `com.example.classB` is in `moduleB`.
 *
 * `moduleC` depends on `moduleA` and `moduleB`.
 * In `moduleC`: there is the link `com.example.classB` where the package `com.example` is in 2 different modules.
 */
class MultiModuleInterModuleLinksIntegrationTest : AbstractGradleIntegrationTest() {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-multimodule-inter-module-links")
        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }
        File(templateProjectDir, "moduleA").copyRecursively(File(projectDir, "moduleA"))
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(LatestTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":moduleA:dokkaHtmlMultiModule",
            "-s",
        ).buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":moduleA:dokkaHtmlMultiModule")).outcome)


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

        // check links to a class from a package that is in moduleB and moduleC
        val moduleDClassPage = outputDir.resolve("moduleD/org.jetbrains.dokka.moduleD/index.html").readText()
        assertTrue(
            moduleDClassPage.contains("""ModuleB: <a href="../../moduleB/org.jetbrains.dokka.it/-module-b/index.html">ModuleB</a>""".trimMargin())
        )
        assertTrue(
            moduleDClassPage.contains("""ModuleC: <a href="../../moduleC/org.jetbrains.dokka.it/-module-c/index.html">ModuleC</a>""".trimMargin())
        )

    }
}
