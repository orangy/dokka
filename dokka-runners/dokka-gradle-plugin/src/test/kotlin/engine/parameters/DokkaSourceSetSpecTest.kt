/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.utils.configureEach_
import org.jetbrains.dokka.gradle.utils.create_
import org.jetbrains.dokka.gradle.utils.enableV2Plugin
import org.jetbrains.dokka.gradle.utils.shouldContainExactly
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class DokkaSourceSetSpecTest : FunSpec({

    context("given a KMP project") {

        test("expect Dokka registers KMP source sets") {
            val project = createKmpProject()
            val dokka = project.extensions.getByType<DokkaExtension>()
            dokka.dokkaSourceSets.names shouldContainExactly listOf(
                "commonMain",
                "commonTest",
                "jvmMain",
                "jvmTest",
                "linuxX64Main",
                "linuxX64Test",
            )
        }

        test("expect test source sets are suppressed by default") {

            val project = createKmpProject()
            val dokka = project.extensions.getByType<DokkaExtension>()
            val mapDssNameToSuppress = dokka.dokkaSourceSets.associate { it.name to it.suppress.orNull }

            mapDssNameToSuppress.shouldContainExactly(
                "commonMain" to false,
                "jvmMain" to false,
                "linuxX64Main" to false,

                "commonTest" to true,
                "jvmTest" to true,
                "linuxX64Test" to true,
            )
        }

        context("DokkaSourceSetSpec.suppress") {

            test("expect convention is 'false'") {
                val dss = createDokkaSourceSet()
                dss.suppress.orNull shouldBe false
            }

            listOf(
                true,
                false,
            ).forEach { newValue ->
                test("can be set to '$newValue'") {
                    val project = createKmpProject()
                    val dokkaSourceSets = project.extensions.getByType<DokkaExtension>().dokkaSourceSets

                    dokkaSourceSets.configureEach_ {
                        suppress.set(newValue)
                    }

                    dokkaSourceSets.shouldForAll {
                        it.suppress.orNull shouldBe newValue
                    }
                }
            }
        }

        context("DokkaSourceSetSpec.analysisPlatform") {
            test("expect convention is 'KotlinPlatform.DEFAULT'") {
                val dss = createDokkaSourceSet()
                dss.analysisPlatform.orNull shouldBe KotlinPlatform.DEFAULT
            }

            KotlinPlatform.values().forEach { newValue ->

                test("can be set to '$newValue'") {
                    val project = createKmpProject()
                    val dokkaSourceSets = project.extensions.getByType<DokkaExtension>().dokkaSourceSets

                    dokkaSourceSets.configureEach_ {
                        analysisPlatform.set(newValue)
                    }

                    dokkaSourceSets.shouldForAll {
                        it.analysisPlatform.orNull shouldBe newValue
                    }
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

private fun createKmpProject(): Project =
    ProjectBuilder.builder().build()
        .enableV2Plugin(v2MigrationHelpers = false)
        .also {

            // Apply KGP, because this will trigger Dokka's KotlinAdapter,
            // which used to be bugged and would prevent overriding 'suppress' and 'analysisPlatform'.
//            it.plugins.apply("org.jetbrains.kotlin.jvm")
            it.plugins.apply("org.jetbrains.kotlin.multiplatform")

            it.extensions.configure<KotlinMultiplatformExtension> {
                jvm().apply {
//                    compilations.create_("dummyAndroid") {
//                        this
//                    }
                }
                linuxX64()
                //androidTarget()
            }

            //it.plugins.apply("com.android.library")

            it.plugins.apply("org.jetbrains.dokka")

//            it.plugins.apply(KotlinAdapter::class)
        }

private fun createDokkaSourceSet(
    project: Project = createKmpProject(),
    configure: DokkaSourceSetSpec.() -> Unit = {}
): DokkaSourceSetSpec {
    val extension = project.extensions.getByType<DokkaExtension>()

    return extension.dokkaSourceSets.create_("Test", configure)
}
