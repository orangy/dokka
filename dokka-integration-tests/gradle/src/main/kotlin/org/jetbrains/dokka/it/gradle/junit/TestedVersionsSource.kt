/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.jetbrains.dokka.it.gradle.junit.TestedVersions.Companion.displayName
import org.jetbrains.dokka.it.gradle.utils.SemVer
import org.jetbrains.dokka.it.gradle.utils.SemVer.Companion.contains
import org.jetbrains.dokka.it.optionalSystemProperty
import org.jetbrains.dokka.it.systemProperty

/**
 * Provide a sequence of [TestedVersions] to re-run tests with different combinations of versions.
 */
fun interface TestedVersionsSource<T : TestedVersions> {

    fun get(): Sequence<T>

    /**
     * Provides [TestedVersions.Default] for a default DGP integration test.
     */
    object Default : TestedVersionsSource<TestedVersions.Default> {

        private val dokkaVersionOverride: String? by optionalSystemProperty()
        private val dokkaVersion: String by systemProperty { dokkaVersionOverride ?: it }

        /**
         * Dokka Gradle Plugin versions to test.
         *
         * This can be overridden via system property [dokkaVersionOverride].
         */
        private val allDokkaGradlePluginVersions: List<String> = listOf(
            dokkaVersion
        )

        /**
         * Kotlin Gradle Plugin versions that DGP will be tested.
         *
         * We aim to support the current and previous two releases.
         *
         * Currently we don't test Kotlin 1.8 because DGPv2 is experimental,
         * with a planned stable release with Kotlin 2.1.
         * When DGPv2 is released as stable, then we will support the last three Kotlin releases.
         */
        private val allKgpVersions: List<String> = listOf(
            "1.9.25",
            "2.0.21",
        )

        /**
         * Gradle versions to test.
         *
         * We test the latest v7 and v8 Gradle versions supported by KGP and Android.
         */
        private val allGradleVersions: List<String> = listOf(
            "7.6.4",
            "8.8.0",
            //"8.10.2", // KGP 2.0.21 does not yet officially support 8.10
        )

        private val allVersions = sequence {
            allDokkaGradlePluginVersions.forEach { dgp ->
                allKgpVersions.forEach { kgp ->
                    allGradleVersions.forEach { gradle ->
                        yield(
                            TestedVersions.Default(
                                dgp = SemVer(dgp),
                                gradle = SemVer(gradle),
                                kgp = SemVer(kgp),
                            )
                        )
                    }
                }
            }
        }

        override fun get(): Sequence<TestedVersions.Default> {
            return allVersions
        }
    }

    /**
     * Provide [TestedVersions.Android] versions to run Android projects.
     *
     * The test must be tagged with [TestsAndroid].
     */
    object Android : TestedVersionsSource<TestedVersions.Android> {
        /**
         * All possible Android Gradle Plugin versions that could be tested.
         *
         * We test the latest v7 and v8 AGP versions.
         *
         * Note the AGP major version indicates the required major version of Gradle.
         * So, AGP 7.* supports Gradle 7, but will throw an error if used with Gradle 8.
         */
        private val allAgpVersions: List<String> = listOf(
            "7.4.2",
            "8.5.2",
        )

        private val allVersions = sequence {
            Default.get().forEach { v ->
                allAgpVersions.forEach { agp ->
                    if (isAgpCompatibleWithGradle(agp = SemVer(agp), gradle = v.gradle)) {
                        yield(
                            TestedVersions.Android(
                                dgp = v.dgp,
                                gradle = v.gradle,
                                kgp = v.kgp,
                                agp = SemVer(agp),
                            )
                        )
                    }
                }
            }
        }

        /**
         * All major versions that _must_ be included in the sequence of all versions.
         *
         * This check is required because some versions are filtered out.
         */
        private val requiredAgpMajorVersions = listOf(7, 8)

        init {
            val agpMajorVersions = allVersions.map { it.agp.major }

            requiredAgpMajorVersions.forEach { requiredAgpMajor ->
                require(requiredAgpMajor in agpMajorVersions) {
                    val versionsList = allVersions.joinToString("\n") { " - ${it.displayName()}" }
                    "Tested versions missing AGP $requiredAgpMajor. All versions:\n$versionsList"
                }
            }
        }

        override fun get(): Sequence<TestedVersions.Android> = allVersions

        private fun isAgpCompatibleWithGradle(agp: SemVer, gradle: SemVer): Boolean {
            // AGP/Gradle compatibility definitions:
            // https://developer.android.com/build/releases/gradle-plugin?buildsystem=ndk-build#updating-gradle
            return when (agp.majorAndMinorVersions) {
                "8.7" -> gradle in "8.9.0"..<"9.0.0"
                "8.6" -> gradle in "8.7.0"..<"9.0.0"
                "8.5" -> gradle in "8.7.0"..<"9.0.0"
                "8.4" -> gradle in "8.6.0"..<"9.0.0"
                "8.3" -> gradle in "8.4.0"..<"9.0.0"
                "8.2" -> gradle in "8.2.0"..<"9.0.0"
                "8.1" -> gradle in "8.0.0"..<"9.0.0"
                "8.0" -> gradle in "8.0.0"..<"9.0.0"
                "7.4" -> gradle in "7.5.0"..<"8.0.0"
                else -> true
            }
        }
    }


    /**
     * Provide [TestedVersions.AndroidCompose] versions to run Android Compose projects.
     *
     * The test must be tagged with [TestsAndroidCompose].
     */
    object AndroidCompose : TestedVersionsSource<TestedVersions.AndroidCompose> {

        /**
         * Versions of the `org.jetbrains.compose` plugins.
         */
        private val allComposeGradlePluginVersions: List<String> = listOf(
            // https://github.com/JetBrains/compose-multiplatform/blob/c52b4a92b94714424dfdd73f6f91a55fec359a69/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/ComposeCompilerCompatibility.kt#L6
            "1.7.0",
        )

        override fun get(): Sequence<TestedVersions.AndroidCompose> = sequence {
            Android.get().forEach { v ->
                allComposeGradlePluginVersions.forEach { composeGP ->
                    yield(
                        TestedVersions.AndroidCompose(
                            dgp = v.dgp,
                            gradle = v.gradle,
                            kgp = v.kgp,
                            agp = v.agp,
                            composeGradlePlugin = SemVer(composeGP),
                        )
                    )
                }
            }
        }
    }
}
