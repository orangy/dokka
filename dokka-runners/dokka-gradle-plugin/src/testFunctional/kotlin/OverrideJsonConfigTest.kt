/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import kotlin.io.path.readText

/**
 * Test [org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask.overrideJsonConfig].
 */
class OverrideJsonConfigTest : FunSpec({

    context("Json Config Override:") {
        val project = createProject()

        test("override works") {

            project.runner
                .addArguments(
                    ":dokkaGenerateModuleHtml",
                    "--rerun",
                )
                .build {
                    output shouldContain "w: [:dokkaGenerateModuleHtml] Overriding DokkaConfiguration with overrideJsonConfig"

                    val actualDokkaConfigJson =
                        project.file("build/tmp/dokkaGenerateModuleHtml/dokka-configuration.json")

                    actualDokkaConfigJson.shouldExist()
                    actualDokkaConfigJson.readText() shouldBe dokkaConfOverrideJson
                }
        }
    }
})

private fun createProject(): GradleProjectTest = gradleKtsProjectTest("OverrideJsonConfigTest") {

    buildGradleKts = """
        |plugins {
        |    kotlin("jvm") version embeddedKotlinVersion
        |    id("org.jetbrains.dokka") version "$DOKKA_VERSION"
        |}
        |
        |tasks.dokkaGenerateModuleHtml {
        |    overrideJsonConfig.set(
        |        providers.fileContents(layout.projectDirectory.file("dokka-config.json")).asText
        |    )
        |}
        |
        """.trimMargin()

    createKotlinFile("src/main/kotlin/Foo.kt", "class Foo")

    createFile("dokka-config.json", dokkaConfOverrideJson)
}

@Language("json")
private val dokkaConfOverrideJson = """
    |{
    |  "moduleName": "Overridden",
    |  "moduleVersion": null,
    |  "outputDir": "blah-blah-override-output",
    |  "cacheRoot": null,
    |  "offlineMode": false,
    |  "sourceSets": [
    |  ],
    |  "pluginsClasspath": [
    |  ],
    |  "pluginsConfiguration": [
    |  ],
    |  "modules": [
    |  ],
    |  "failOnWarning": false,
    |  "delayTemplateSubstitution": false,
    |  "suppressObviousFunctions": true,
    |  "includes": [
    |  ],
    |  "suppressInheritedMembers": false,
    |  "finalizeCoroutines": false
    |}
    """.trimMargin()
