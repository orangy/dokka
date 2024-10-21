/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.tasks

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.dokka.gradle.internal.DokkaConstants
import org.jetbrains.dokka.gradle.utils.*
import kotlin.io.path.appendLines
import kotlin.io.path.copyTo

class LogHtmlPublicationLinkTaskTest : FunSpec({

    // Re-run all leaves, to ensure each test has a fresh server.
    //isolationMode = InstancePerLeaf

    context("given an active file-host server") {
        val server = embeddedServer(CIO, port = 0) { }
        afterSpec { server.stop(gracePeriodMillis = 0, timeoutMillis = 0) }
        server.start(wait = false)
        val serverPort = server.resolvedConnectors().first().port

        val validServerUri = "http://localhost:$serverPort"
        val validServerUriParam = "testServerUri=$validServerUri"

        context("and a Kotlin project") {
            val project = initDokkaProject()

            context("when generate task is run with correct server URI") {
                project.runner
                    .addArguments(
                        "clean",
                        "dokkaGeneratePublicationHtml",
                        "--stacktrace",
                        "-P${validServerUriParam}",
                    )
                    .build {
                        test("expect project builds successfully") {
                            output shouldContain "BUILD SUCCESSFUL"
                        }
                        test("LogHtmlPublicationLinkTask should run") {
                            shouldHaveTasksWithAnyOutcome(
                                ":logLinkDokkaGeneratePublicationHtml" to listOf(SUCCESS)
                            )
                        }
                        test("expect link is logged") {
                            output shouldContain "Generated Dokka HTML publication: $validServerUri/log-html-publication-link-task/build/dokka/html/index.html"
                        }
                    }
            }

            context("and the server is down") {
                val invalidServerUriParam = "testServerUri=http://notavailable.test:"
                context("when running the generate task") {
                    project.runner
                        .addArguments(
                            "clean",
                            "dokkaGeneratePublicationHtml",
                            "--stacktrace",
                            "-P${invalidServerUriParam}",
                        )
                        .build {
                            test("expect project builds successfully") {
                                output shouldContain "BUILD SUCCESSFUL"
                            }
                            test("LogHtmlPublicationLinkTask should be skipped") {
                                shouldHaveTasksWithAnyOutcome(
                                    ":logLinkDokkaGeneratePublicationHtml" to listOf(SKIPPED)
                                )
                                output shouldContain "Skipping task ':logLinkDokkaGeneratePublicationHtml' as task onlyIf 'server URL is reachable' is false"
                            }
                            test("expect link is not logged") {
                                output shouldNotContain "Generated Dokka HTML publication"
                            }
                        }
                }
            }
        }

        context("and a composite build project") {
            val project = initCompositeBuildDokkaProject()

            val nestedProjectGradleProperties = project.file("nested-project/gradle.properties")
            project.file("gradle.properties")
                .copyTo(
                    target = nestedProjectGradleProperties,
                    overwrite = true,
                )
            nestedProjectGradleProperties.appendLines(
                listOf(
                    validServerUriParam
                )
            )

            context("when generate task is run with correct server URI") {
                project.runner
                    .addArguments(
                        "clean",
                        "dokkaGenerate",
                        "--stacktrace",
                    )
                    .build {
                        test("expect project builds successfully") {
                            output shouldContain "BUILD SUCCESSFUL"
                        }
                        test("LogHtmlPublicationLinkTask should run") {
                            shouldHaveTasksWithAnyOutcome(
                                ":nested-project:logLinkDokkaGeneratePublicationHtml" to listOf(SUCCESS)
                            )
                        }
                        test("expect link is logged") {
                            output shouldContain "Generated Dokka HTML publication: $validServerUri/Composite%20Build%20Demo/nested-project/build/dokka/html/index.html"
                        }
                    }
            }
        }
    }
})


private fun initDokkaProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    return gradleKtsProjectTest("log-html-publication-link-task") {
        buildGradleKts = """
            |plugins {
            |  kotlin("jvm") version "1.8.22"
            |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
            |}
            |
            |tasks.withType<org.jetbrains.dokka.gradle.tasks.LogHtmlPublicationLinkTask>().configureEach {
            |  serverUri.set(providers.gradleProperty("testServerUri"))
            |}
            """.trimMargin()

        createKotlinFile(
            "src/main/kotlin/Hello.kt",
            """
            |package com.project.hello
            |
            |/** The Hello class */
            |class Hello {
            |    /** prints `Hello` to the console */  
            |    fun sayHello() = println("Hello")
            |}
            |
            """.trimMargin()
        )

        // remove the flag that disables the logging task, since this test wants the logger to run
        gradleProperties.dokka.enableLogHtmlPublicationLink = null

        config()
    }
}


private fun initCompositeBuildDokkaProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    return gradleKtsProjectTest(
        projectLocation = "log-html-publication-link-task-composite-build",
        rootProjectName = "Composite Build Demo"
    ) {
        buildGradleKts = """
            |plugins {
            |  base
            |}
            |
            |tasks.clean {
            |  dependsOn(gradle.includedBuild("nested-project").task(":clean"))
            |}
            |
            |val dokkaGenerate by tasks.registering {
            |  dependsOn(gradle.includedBuild("nested-project").task(":dokkaGenerate"))
            |}
            |
            """.trimMargin()

        settingsGradleKts += """
            |includeBuild("nested-project")
        """.trimMargin()

        dir("nested-project") {
            buildGradleKts = """
                |plugins {
                |  kotlin("jvm") version "1.9.25"
                |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
                |}
                |
                |tasks.withType<org.jetbrains.dokka.gradle.tasks.LogHtmlPublicationLinkTask>().configureEach {
                |  serverUri.set(providers.gradleProperty("testServerUri"))
                |}
                """.trimMargin()

            settingsGradleKts = """
                |rootProject.name = "Nested Project"
                |
                |${settingsRepositories()}
                |
            """.trimMargin()

            createKotlinFile(
                "src/main/kotlin/Hello.kt",
                """
                |package com.project.hello
                |
                |/** The Hello class */
                |class Hello {
                |    /** prints `Hello` to the console */  
                |    fun sayHello() = println("Hello")
                |}
                |
                """.trimMargin()
            )
        }

        // remove the flag that disables the logging task, since this test wants the logger to run
        gradleProperties.dokka.enableLogHtmlPublicationLink = null

        config()
    }
}
