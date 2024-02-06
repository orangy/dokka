/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.*

class BasicGradleIntegrationTest : AbstractGradleIntegrationTest() {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-basic")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
        val customResourcesDir = File(templateProjectDir, "customResources")

        if (customResourcesDir.exists() && customResourcesDir.isDirectory) {
            val destination = File(projectDir.parentFile, "customResources")
            destination.mkdirs()
            destination.deleteRecursively()
            customResourcesDir.copyRecursively(destination)
        }
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AllSupportedTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        runAndAssertOutcome(buildVersions, TaskOutcome.SUCCESS)
        runAndAssertOutcome(buildVersions, TaskOutcome.UP_TO_DATE)
    }

    private fun runAndAssertOutcome(buildVersions: BuildVersions, expectedOutcome: TaskOutcome) {
        val result = createGradleRunner(
            buildVersions,
            "dokkaHtml",
            "dokkaJavadoc",
            "dokkaGfm",
            "dokkaJekyll",
//            "-i",
            "-s"
        ).buildRelaxed()

        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaHtml")).outcome)
        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaJavadoc")).outcome)
        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaGfm")).outcome)
        assertEquals(expectedOutcome, assertNotNull(result.task(":dokkaJekyll")).outcome)

        File(projectDir, "build/dokka/html").assertHtmlOutputDir()
        File(projectDir, "build/dokka/javadoc").assertJavadocOutputDir()
        File(projectDir, "build/dokka/gfm").assertGfmOutputDir()
        File(projectDir, "build/dokka/jekyll").assertJekyllOutputDir()
    }

    private fun File.assertHtmlOutputDir() {
        assertTrue(isDirectory, "Missing dokka html output directory")

        val imagesDir = File(this, "images")
        assertTrue(imagesDir.isDirectory, "Missing images directory")

        val scriptsDir = File(this, "scripts")
        assertTrue(scriptsDir.isDirectory, "Missing scripts directory")
        val reactFile = File(this, "scripts/main.js")
        assertTrue(reactFile.isFile, "Missing main.js")

        val stylesDir = File(this, "styles")
        assertTrue(stylesDir.isDirectory, "Missing styles directory")
        val reactStyles = File(this, "styles/main.css")
        assertTrue(reactStyles.isFile, "Missing main.css")

        val navigationHtml = File(this, "navigation.html")
        assertTrue(navigationHtml.isFile, "Missing navigation.html")

        val moduleOutputDir = File(this, "-basic -project")
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
            assertNoSuppressedMarker(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }

        assertTrue(
            allHtmlFiles().any { file -> "Basic Project" in file.readText() },
            "Expected configured moduleName to be present in html"
        )

        assertTrue(
            allHtmlFiles().any { file ->
                "https://github.com/Kotlin/dokka/tree/master/" +
                        "dokka-integration-tests/gradle/projects/it-basic/" +
                        "src/main/kotlin/it/basic/PublicClass.kt" in file.readText()
            },
            "Expected `PublicClass` source link to GitHub"
        )

        assertTrue(
            allHtmlFiles().any { file ->
                "https://github.com/Kotlin/dokka/tree/master/" +
                        "dokka-integration-tests/gradle/projects/it-basic/" +
                        "src/main/java/it/basic/java/SampleJavaClass.java" in file.readText()
            },
            "Expected `SampleJavaClass` source link to GitHub"
        )

        val anchorsShouldNotHaveHashes = "<a data-name=\".*#.*\"\\sanchor-label=\"*.*\">".toRegex()
        assertTrue(
            allHtmlFiles().all { file ->
                !anchorsShouldNotHaveHashes.containsMatchIn(file.readText())
            },
            "Anchors should not have hashes inside"
        )

        assertTrue(
            stylesDir.resolve("logo-styles.css").readText().contains(
                "--dokka-logo-image-url: url('https://upload.wikimedia.org/wikipedia/commons/9/9d/Ubuntu_logo.svg');",
            )
        )
        assertTrue(stylesDir.resolve("custom-style-to-add.css").isFile)
        assertTrue(stylesDir.resolve("custom-style-to-add.css").readText().contains("/* custom stylesheet */"))
        allHtmlFiles().forEach { file ->
            if (file.name != "navigation.html") assertTrue(
                "custom-style-to-add.css" in file.readText(),
                "custom styles not added to html file ${file.name}"
            )
        }
        assertTrue(imagesDir.resolve("custom-resource.svg").isFile)

        assertConfiguredVisibility(this)
    }

    private fun File.assertJavadocOutputDir() {
        assertTrue(isDirectory, "Missing dokka javadoc output directory")

        val indexFile = File(this, "index.html")
        assertTrue(indexFile.isFile, "Missing index.html")
        assertTrue(
            """<title>Basic Project 2.0.0-SNAPSHOT API </title>""" in indexFile.readText(),
            "Header with version number not present in index.html"
        )

        assertTrue {
            allHtmlFiles().all {
                "0.0.1" !in it.readText()
            }
        }
    }

    private fun File.assertGfmOutputDir() {
        assertTrue(isDirectory, "Missing dokka gfm output directory")
    }

    private fun File.assertJekyllOutputDir() {
        assertTrue(isDirectory, "Missing dokka jekyll output directory")
    }

    private fun assertConfiguredVisibility(outputDir: File) {
        val allHtmlFiles = outputDir.allHtmlFiles().toList()

        assertContentVisibility(
            contentFiles = allHtmlFiles,
            documentPublic = true,
            documentProtected = true, // sourceSet documentedVisibilities
            documentInternal = false,
            documentPrivate = true // for overriddenVisibility package
        )

        assertContainsFilePaths(
            outputFiles = allHtmlFiles,
            expectedFilePaths = listOf(
                // documentedVisibilities is overridden for package `overriddenVisibility` specifically
                // to include private code, so html pages for it are expected to have been created
                Regex("it\\.overriddenVisibility/-visible-private-class/private-method\\.html"),
                Regex("it\\.overriddenVisibility/-visible-private-class/private-val\\.html"),
            )
        )
    }
}
