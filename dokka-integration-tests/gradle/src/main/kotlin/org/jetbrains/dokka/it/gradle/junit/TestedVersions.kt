/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.jetbrains.dokka.it.gradle.utils.SemVer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * The parameters used to run each invocation of a [DokkaGradlePluginTest] test.
 *
 * [DokkaGradlePluginTestExtension] generates all possible values, and creates a test invocation
 * for each valid combination.
 *
 * To access the specific values used for a test invocation, add a [TestedVersions]
 * parameter in the test function.
 */
sealed interface TestedVersions : Comparable<TestedVersions> {

    /** Dokka Gradle Plugin version. */
    val dgp: SemVer

    /** Gradle version. */
    val gradle: SemVer

    /** Kotlin Gradle Plugin version. */
    val kgp: SemVer


    sealed interface WithAndroid : TestedVersions {
        /**
         * The Android Gradle Plugin version being tested.
         */
        val agp: SemVer
    }


    sealed interface WithCompose : TestedVersions {
        /**
         * The Compose version being tested.
         */
        val composeGradlePlugin: SemVer
    }

    /**
     * The versions used for each invocation of a [DokkaGradlePluginTest] test.
     */
    data class Default(
        override val dgp: SemVer,
        override val gradle: SemVer,
        override val kgp: SemVer,
    ) : TestedVersions

    /**
     * The versions used for each invocation of a [DokkaGradlePluginTest] test
     * with an Android project.
     *
     * The test (or test class) must be annotated with [TestsAndroid] for
     * [DokkaGradlePluginTestExtension] to generate a project using these versions
     */
    data class Android(
        override val agp: SemVer,
        override val dgp: SemVer,
        override val gradle: SemVer,
        override val kgp: SemVer,
    ) : WithAndroid


    /**
     * The versions used for each invocation of a [DokkaGradlePluginTest] test
     * with an Android Compose project.
     *
     * The test (or test class) must be annotated with [TestsAndroidCompose] for
     * [DokkaGradlePluginTestExtension] to generate a project using these versions
     */
    data class AndroidCompose(
        override val agp: SemVer,
        override val composeGradlePlugin: SemVer,
        override val dgp: SemVer,
        override val gradle: SemVer,
        override val kgp: SemVer,
    ) : WithAndroid, WithCompose


    override fun compareTo(other: TestedVersions): Int {
        toMap().entries
            .sortedBy { it.key }
            .forEach { (key, thisVersion) ->
                val otherVersion = other.toMap()[key] ?: return 1
                val cmp = thisVersion.compareTo(otherVersion)
                if (cmp != 0) {
                    return cmp
                }
            }
        return 0
    }

    companion object {
        /**
         * Join all versions in a dash-separated-string, for use in file names.
         */
        fun TestedVersions.dashSeparatedId(): String =
            toMap().entries
                .map { (key, value) -> "$key-$value" }
                .sorted()
                .joinToString("_")

        /**
         * Pretty display name containing all versions, for use in JUnit test names.
         */
        fun TestedVersions.displayName(): String =
            toMap().entries
                .map { (key, value) -> "$key: $value" }
                .sorted()
                .joinToString(", ")

        private fun TestedVersions.toMap(): Map<String, SemVer> =
            buildMap {
                put(::dgp.name, dgp)
                put(::gradle.name, gradle)
                put(::kgp.name, kgp)
                when (this@toMap) {
                    is Android -> {
                        put(::agp.name, agp)
                    }

                    is AndroidCompose -> {
                        put(::agp.name, agp)
                        put(::composeGradlePlugin.name, composeGradlePlugin)
                    }

                    is Default -> {} // Default does not have any specific values.
                }
            }

        /**
         * Returns `true` if [this] is an instance of [TestedVersions.WithAndroid].
         */
        @OptIn(ExperimentalContracts::class)
        fun TestedVersions.hasAgp(): Boolean {
            contract { returns(true) implies (this@hasAgp is WithAndroid) }
            return this is WithAndroid
        }

        /**
         * Returns `true` if [this] is an instance of [TestedVersions.WithCompose].
         */
        @OptIn(ExperimentalContracts::class)
        fun TestedVersions.hasCompose(): Boolean {
            contract { returns(true) implies (this@hasCompose is WithCompose) }
            return this is WithCompose
        }
    }
}
