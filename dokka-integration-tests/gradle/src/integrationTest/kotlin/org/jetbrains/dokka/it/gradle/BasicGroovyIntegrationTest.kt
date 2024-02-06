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

class BasicGroovyIntegrationTest : AbstractGradleIntegrationTest() {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-basic-groovy")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(buildVersions, "dokkaHtml", "dokkaJavadoc", "dokkaGfm", "dokkaJekyll", "-s")
            .buildRelaxed()

        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtml")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaJavadoc")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaGfm")).outcome)
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaJekyll")).outcome)

        File(projectDir, "build/dokka/customHtml").assertKdocOutputDir()
        File(projectDir, "build/dokka/customJavadoc").assertJavadocOutputDir()
        File(projectDir, "build/dokka/customGfm").assertGfmOutputDir()
        File(projectDir, "build/dokka/customJekyll").assertJekyllOutputDir()
    }

    private fun File.assertKdocOutputDir() {
        assertTrue(isDirectory, "Missing dokka html output directory")

        val imagesDir = File(this, "images")
        assertTrue(imagesDir.isDirectory, "Missing images directory")

        val scriptsDir = File(this, "scripts")
        assertTrue(scriptsDir.isDirectory, "Missing scripts directory")

        val stylesDir = File(this, "styles")
        assertTrue(stylesDir.isDirectory, "Missing styles directory")

        val navigationHtml = File(this, "navigation.html")
        assertTrue(navigationHtml.isFile, "Missing navigation.html")

        val moduleOutputDir = File(this, "it-basic-groovy")
        assertTrue(moduleOutputDir.isDirectory, "Missing module directory")

        val moduleIndexHtml = File(this, "index.html")
        assertTrue(moduleIndexHtml.isFile, "Missing module index.html")

        val modulePackageDir = File(moduleOutputDir, "it.basic")
        assertTrue(modulePackageDir.isDirectory, "Missing it.basic package directory")

        val modulePackageIndexHtml = File(modulePackageDir, "index.html")
        assertTrue(modulePackageIndexHtml.isFile, "Missing module package index.html")

        val moduleJavaPackageDir = File(moduleOutputDir, "it.basic.java")
        assertTrue(moduleJavaPackageDir.isDirectory, "Missing it.basic.java package directory")

        allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }

    private fun File.assertJavadocOutputDir() {
        assertTrue(isDirectory, "Missing dokka javadoc output directory")
    }

    private fun File.assertGfmOutputDir() {
        assertTrue(isDirectory, "Missing dokka gfm output directory")
    }

    private fun File.assertJekyllOutputDir() {
        assertTrue(isDirectory, "Missing dokka jekyll output directory")
    }
}
