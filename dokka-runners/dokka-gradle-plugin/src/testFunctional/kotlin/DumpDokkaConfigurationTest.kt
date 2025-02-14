/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.file.shouldExist
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.name
import kotlin.io.path.relativeToOrSelf

class DumpDokkaConfigurationTest : FunSpec({

    context("enabling dumpDokkaConfigurationDebugFile should dump configuration to file") {

        val project = initProject()

        project.gradleProperties {
            dokka {
                dumpDokkaConfigurationDebugFile = true
            }
        }

        project.runner
            .addArguments(
                ":dokkaGenerateModuleHtml",
                ":dokkaGeneratePublicationHtml",
                "--rerun-tasks",
            )
            .forwardOutput()
            .build {
                test("expect dokka tasks run successfully") {
                    shouldHaveTasksWithOutcome(
                        ":dokkaGenerateModuleHtml" to SUCCESS,
                        ":dokkaGeneratePublicationHtml" to SUCCESS,
                    )
                }

                test("expect dokka tasks generate config files") {
                    project.dir("build/tmp")
                        .findFiles { it.name == "dokka-configuration.json" }
                        .map { it.relativeToOrSelf(project.projectDir).invariantSeparatorsPathString }
                        .toList()
                        .shouldContainExactlyInAnyOrder(
                            "build/tmp/dokkaGeneratePublicationHtml/dokka-configuration.json",
                            "build/tmp/dokkaGenerateModuleHtml/dokka-configuration.json"
                        )
                }
            }
    }

    context("implicit task outputs should not contain Dokka config file") {
        listOf(
            "dumpDokkaConfigurationDebugFile is not set" to null,
            "dumpDokkaConfigurationDebugFile is disabled" to false,
        ).forEach { (testName, value) ->
            context("when $testName ") {
                val project = initProject()

                project.gradleProperties {
                    dokka {
                        dumpDokkaConfigurationDebugFile = value
                    }
                }

                project.runner
                    .forwardOutput()
                    .addArguments(
                        ":syncDokkaOutput",
                        "--rerun",
                    )
                    .build {
                        test("expect syncDokkaOutput runs successfully") {
                            shouldHaveTasksWithOutcome(
                                ":syncDokkaOutput" to SUCCESS,
                            )
                        }

                        test("verify sync task collects Dokka output files") {
                            // Just a simple check to make sure that some files were copied, because testing the config
                            // files don't exist could mean that nothing was copied and something else broke.
                            project.file("build/tmp/syncDokkaOutput/module/module-descriptor.json")
                                .toFile()
                                .shouldExist()
                            project.file("build/tmp/syncDokkaOutput/publication/index.html")
                                .toFile()
                                .shouldExist()
                        }

                        test("expect no Dokka config file in output") {
                            project
                                .dir("build/tmp/syncDokkaOutput")
                                .findFiles { it.name == "dokka-configuration.json" }
                                .map { it.relativeToOrSelf(project.projectDir).invariantSeparatorsPathString }
                                .toList()
                                .shouldBeEmpty()
                        }
                    }
            }
        }
    }
})

private fun TestScope.initProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    val baseDirName = testCase.descriptor.ids()
        .joinToString("/") { it.value.substringAfter("org.jetbrains.dokka.gradle.").replaceNonAlphaNumeric() }

    return gradleKtsProjectTest(
        projectLocation = baseDirName,
        rootProjectName = "DumpDokkaConfigurationTest",
    ) {
        buildGradleKts = """
            |plugins {
            |    kotlin("jvm") version embeddedKotlinVersion
            |    id("org.jetbrains.dokka") version "$DOKKA_VERSION"
            |}
            |
            |val syncDokkaOutput by tasks.registering(Sync::class) {
            |    // fetch the implicit output files of the DokkaGenerate tasks
            |    from(tasks.dokkaGeneratePublicationHtml) {
            |       into("publication")
            |    }
            |    from(tasks.dokkaGenerateModuleHtml) {
            |       into("module")
            |    }
            |    into(temporaryDir)
            |}
            |
            """.trimMargin()

        createKotlinFile("src/main/kotlin/Foo.kt", "class Foo")

        config()
    }
}
