/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.examples

import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.jetbrains.dokka.gradle.utils.*
import org.jetbrains.dokka.it.gradle.loadConfigurationCacheReportData
import org.jetbrains.dokka.it.systemProperty
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.*
import kotlin.streams.asStream

class ExampleProjectsTest {

    class TestCaseProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext): Stream<Arguments> {
            val exampleProjectDirs = exampleGradleProjectsDir
                .listDirectoryEntries()
                .filter { it.isDirectory() }

            val temporaryDir = createTempDirectory()

            return exampleProjectDirs
                .asSequence()
                .map { exampleProjectDir ->
                    val projectDestinationDir = temporaryDir.resolve(exampleProjectDir.name)

                    val exampleProject = initProject(exampleProjectDir, projectDestinationDir)
                    val expectedDataDir = expectedDataDir.resolve("exampleProjects").resolve(exampleProjectDir.name)

                    named(
                        exampleProjectDir.name,
                        TestCase(
                            project = exampleProject,
                            expectedDataDir = expectedDataDir,
                        )
                    )
                }
                .map { arguments(it) }
                .asStream()
        }

        companion object {
            /** Base directory that contains all Dokka Gradle example projects. */
            private val exampleGradleProjectsDir by systemProperty(::Path)

            /** Base directory that contains all expected output data for the Gradle example projects. */
            private val expectedDataDir by systemProperty(::Path)

            /** Create a new [GradleProjectTest] copied from the source project in [sourceProjectDir]. */
            private fun initProject(
                sourceProjectDir: Path,
                destinationDir: Path,
            ): GradleProjectTest {
                return GradleProjectTest(destinationDir).apply {
                    sourceProjectDir.copyToRecursively(projectDir, overwrite = true, followLinks = false)
                    updateSettingsRepositories()
                }
            }
        }
    }

    data class TestCase(
        val project: GradleProjectTest,
        val expectedDataDir: Path,
    ) {
        val exampleProjectName = ExampleProject.of(project.projectDir)

        /** `true` if the project produces Dokka HTML. */
        val outputsHtml: Boolean =
            when (exampleProjectName) {
                ExampleProject.Javadoc -> false
                else -> true
            }

        /** `true` if the project produces Dokka Javadoc. */
        val outputsJavadoc: Boolean =
            when (exampleProjectName) {
                ExampleProject.Javadoc -> true
                ExampleProject.LibraryPublishing -> true
                else -> false
            }

        /** Base directory for the  */
        private val dokkaOutputPath: String =
            when (exampleProjectName) {
                ExampleProject.VersioningMultimodule -> "docs/build/dokka/"
                ExampleProject.Multimodule -> "docs/build/dokka/"
                ExampleProject.CompositeBuild -> "docs/build/dokka/"
                else -> "build/dokka/"
            }

        val dokkaOutputDir: Path = project.projectDir.resolve(dokkaOutputPath)

        val dokkaGenerateTask: String = when (exampleProjectName) {
            ExampleProject.VersioningMultimodule -> ":docs:dokkaGenerate"
            ExampleProject.Multimodule -> ":docs:dokkaGenerate"
            ExampleProject.CompositeBuild -> ":build"
            else -> ":dokkaGenerate"
        }
    }

    /**
     * Identifier for each example project, to avoid string comparisons everywhere.
     */
    enum class ExampleProject {
        BasicGradle,
        CompositeBuild,
        CustomStyling,
        Java,
        Javadoc,
        KotlinAsJava,
        KotlinMultiplatform,
        LibraryPublishing,
        Multimodule,
        VersioningMultimodule,
        ;

        companion object {
            fun of(dir: Path): ExampleProject {
                return when (dir.name) {
                    "basic-gradle-example" -> BasicGradle
                    "javadoc-example" -> Javadoc
                    "composite-build-example" -> CompositeBuild
                    "custom-styling-example" -> CustomStyling
                    "java-example" -> Java
                    "kotlin-as-java-example" -> KotlinAsJava
                    "kotlin-multiplatform-example" -> KotlinMultiplatform
                    "library-publishing-example" -> LibraryPublishing
                    "multimodule-example" -> Multimodule
                    "versioning-multimodule-example" -> VersioningMultimodule
                    else -> error("Undeclared example project: $dir")
                }
            }
        }
    }


    @ParameterizedTest
    @ArgumentsSource(TestCaseProvider::class)
    fun `test HTML output`(testCase: TestCase) {
        assumeTrue(testCase.outputsHtml)

        testDokkaOutput(
            testCase = testCase,
            format = "html",
        )
    }

    @ParameterizedTest
    @ArgumentsSource(TestCaseProvider::class)
    fun `test Javadoc output`(testCase: TestCase) {
        assumeTrue(testCase.outputsJavadoc)

        testDokkaOutput(
            testCase = testCase,
            format = "javadoc",
        )
    }

    private fun testDokkaOutput(
        testCase: TestCase,
        format: String,
    ) {
        val expectedDataDir = testCase.expectedDataDir.resolve(format)
        val dokkaOutputDir = testCase.dokkaOutputDir.resolve(format)

        assert(expectedDataDir.isDirectory()) {
            "Missing expectedDataDir: ${expectedDataDir.toUri()}"
        }

        testCase.project.runner
            .addArguments(
                testCase.dokkaGenerateTask,
                "--stacktrace",
            )
            .build {
                dokkaOutputDir.shouldBeADirectory()

                withClue({
                    """
                    expectedDataDir: ${expectedDataDir.toUri()}
                    actualOutputDir: ${dokkaOutputDir.toUri()}
                    """.trimIndent()
                }) {
                    withClue("expect file trees are the same") {
                        val expectedFileTree = expectedDataDir.toTreeString()
                        val actualFileTree = dokkaOutputDir.toTreeString()
                        expectedFileTree shouldBe actualFileTree
                    }

                    withClue("expect directories are the same") {
                        dokkaOutputDir shouldBeADirectoryWithSameContentAs expectedDataDir
                    }
                }
            }
    }


    @ParameterizedTest
    @ArgumentsSource(TestCaseProvider::class)
    fun `test build cache`(
        testCase: TestCase,
        @TempDir tempDir: Path,
    ) {

        val buildCacheDir = tempDir.resolve("build-cache").apply {
            deleteRecursively()
            createDirectories()
        }

        testCase.project.settingsGradleKts += """
            |buildCache {
            |    local {
            |        directory = File("${buildCacheDir.invariantSeparatorsPathString}")
            |    }
            |}
        """.trimMargin()

        // Initial build, to populate the build cache.
        testCase.project.runner
            .addArguments(
                testCase.dokkaGenerateTask,
                "--stacktrace",
            )
            .build {
                output shouldContain "BUILD SUCCESSFUL"
            }

        // Clean local directories, to ensure tasks are loaded from build cache.
        testCase.project.runner
            .addArguments(
                "clean",
                "--stacktrace",
            )
            .build {
                output shouldContain "BUILD SUCCESSFUL"
            }

        // Re-run generate, and verify that the Dokka tasks are cached.
        testCase.project.runner
            .addArguments(
                testCase.dokkaGenerateTask,
                "--stacktrace",
                "--build-cache",
            )
            .build {
                when (testCase.exampleProjectName) {

                    ExampleProject.Javadoc -> {
                        shouldHaveTasksWithOutcome(
                            ":dokkaGeneratePublicationJavadoc" to FROM_CACHE,
                            ":dokkaGenerateModuleJavadoc" to FROM_CACHE,
                            ":dokkaGenerate" to UP_TO_DATE,
                        )
                    }

                    ExampleProject.CompositeBuild -> {
                        shouldHaveTasksWithOutcome(
                            ":module-kakapo:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":module-kea:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":docs:dokkaGeneratePublicationHtml" to FROM_CACHE,
                            ":docs:dokkaGenerate" to UP_TO_DATE,
                        )
                    }

                    ExampleProject.Multimodule -> {
                        shouldHaveTasksWithOutcome(
                            ":docs:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":childProjectA:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":childProjectB:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":docs:dokkaGeneratePublicationHtml" to FROM_CACHE,
                            ":docs:dokkaGenerate" to UP_TO_DATE,
                        )
                    }

                    ExampleProject.VersioningMultimodule -> {
                        shouldHaveTasksWithOutcome(
                            ":docs:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":childProjectA:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":childProjectB:dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":docs:dokkaGeneratePublicationHtml" to FROM_CACHE,
                            ":docs:dokkaGenerate" to UP_TO_DATE,
                        )
                    }

                    else -> {
                        shouldHaveTasksWithOutcome(
                            ":dokkaGeneratePublicationHtml" to FROM_CACHE,
                            ":dokkaGenerateModuleHtml" to FROM_CACHE,
                            ":dokkaGenerate" to UP_TO_DATE,
                        )
                    }
                }
            }
    }


    @ParameterizedTest
    @ArgumentsSource(TestCaseProvider::class)
    fun `test configuration cache`(testCase: TestCase) {
        // delete old configuration cache results and reports, to make sure we can fetch the newest report
        testCase.project.findFiles {
            val isCCDir = it.invariantSeparatorsPath.endsWith(".gradle/configuration-cache")
            val isCCReportDir = it.invariantSeparatorsPath.endsWith("build/reports/configuration-cache")
            it.isDirectory && (isCCReportDir || isCCDir)
        }.forEach { it.deleteRecursively() }

        val configCacheRunner: GradleRunner =
            testCase.project.runner
                .addArguments(
                    testCase.dokkaGenerateTask,
                    "--stacktrace",
                    "--configuration-cache",
                )

        //first build should store the configuration cache
        configCacheRunner.build {
            output shouldContain "BUILD SUCCESSFUL"
            output shouldContain "Configuration cache entry stored"

            val ccReport = testCase.project.loadConfigurationCacheReportData().shouldNotBeNull()
            ccReport.asClue {
                it.totalProblemCount shouldBe 0
            }
        }

        // second build should reuse the configuration cache
        configCacheRunner.build {
            output shouldContain "BUILD SUCCESSFUL"
            output shouldContain "Configuration cache entry reused"
        }
    }
}
