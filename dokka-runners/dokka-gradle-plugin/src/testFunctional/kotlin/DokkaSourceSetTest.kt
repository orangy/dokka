/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.shouldBe
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.internal.DokkaConstants.DOKKA_VERSION
import org.jetbrains.dokka.gradle.utils.*
import kotlin.io.path.readLines

class DokkaSourceSetTest : FunSpec({

    context("given an Android KMP project") {

        val project = initAndroidKmpProject()

        context("DokkaSourceSetSpec.analysisPlatform") {

            KotlinPlatform.values().forEach { newValue ->

                test("can be set to '$newValue'") {

                    project.runner
                        .addArguments(
                            ":dokkaGenerate",
                            "-PanalysisPlatformOverride=$newValue",
                        )
                        .build {

                            val dokkaConfigJson =
                                project.file("build/tmp/dokkaGeneratePublicationHtml/dokka-configuration.json")

                            dokkaConfigJson.readLines()
                                .filter { it.contains("analysisPlatform") }
                                .shouldForAll { line ->
                                    line shouldBe """
                                        "analysisPlatform": "$newValue",
                                    """.trimIndent()
                                }
                        }

//                val project = createKmpProject()
//                val dokkaSourceSets = project.extensions.getByType<DokkaExtension>().dokkaSourceSets
//
//                dokkaSourceSets.configureEach_ {
//                    analysisPlatform.set(newValue)
//                }
//
//                dokkaSourceSets.shouldForAll {
//                    it.analysisPlatform.orNull shouldBe newValue
//                }
                }

//            test("can be set to '$newValue'") {
//                val project = createProject()
//                val dss = project.extensions.getByType<DokkaExtension>().dokkaSourceSets.getByName("commonMain")
//                dss.analysisPlatform.set(newValue)
//                dss.analysisPlatform.orNull shouldBe newValue
//            }
            }
        }
    }
})


private fun initAndroidKmpProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {
    return gradleKtsProjectTest("dokka-source-sets-test") {

        settingsGradleKts = settingsGradleKts.replace("mavenCentral()", "mavenCentral(); google()")

        buildGradleKts = """
|plugins {
|    kotlin("multiplatform") version embeddedKotlinVersion
|    id("com.android.library") version "8.7.2"
|    id("org.jetbrains.dokka") version "$DOKKA_VERSION"
|}
|
|kotlin {
|    jvmToolchain(17)
|    jvm()
|    linuxX64()
|    androidTarget()
|}
|
|android {
|    namespace = "x.y.z.demo"
|}
|
|dokka {
|    dokkaSourceSets.configureEach {
|        providers.gradleProperty("analysisPlatformOverride").orNull?.let { newValue ->
|            analysisPlatform.set(newValue)
|        }
|    }
|}
|
""".trimMargin()

        dir("src/commonMain/kotlin") {
            createFile("Foo.kt", "class Foo")
        }

        config()
    }
}
