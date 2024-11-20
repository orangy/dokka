/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.jetbrains.dokka.it.gradle.AbstractGradleIntegrationTest.Companion.updateProjectLocalMavenDir
import org.jetbrains.dokka.it.gradle.junit.DokkaGradlePluginTestExtension.Companion.getAndroidSdkDir
import org.jetbrains.dokka.it.gradle.junit.TestedVersions.Companion.hasAgp
import org.jetbrains.dokka.it.gradle.junit.TestedVersions.Companion.hasCompose
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Initialise a Gradle Test project into a directory.
 *
 * The source directory must not be modified.
 */
fun interface GradleTestProjectInitializer {

    /**
     * Must copy the contents of [source] into [destination].
     *
     * Should create a `gradle.properties` file with the contents of [gradleProperties].
     */
    fun initialize(
        source: Path,
        destination: Path,
        testedVersions: TestedVersions,
        gradleProperties: Map<String, String>,
    )

    object Default : GradleTestProjectInitializer {
        override fun initialize(
            source: Path,
            destination: Path,
            testedVersions: TestedVersions,
            gradleProperties: Map<String, String>,
        ) {
            require(source.isDirectory()) { "Source project must be a directory. Actual: $source" }
            source.copyToRecursively(destination, overwrite = true, followLinks = false)

            destination.updateProjectLocalMavenDir()

            createGradlePropertiesFile(destination, gradleProperties)

            updateVersions(destination, testedVersions)

            if (testedVersions.hasAgp()) {
                initialiseAndroid(
                    destination = destination
                )
            }
        }

        private fun createGradlePropertiesFile(
            projectDir: Path,
            gradleProperties: Map<String, String>,
        ) {
            val gradlePropertiesFile = projectDir.resolve("gradle.properties").apply {
                deleteIfExists()
                createFile()
            }

            gradlePropertiesFile.writeText(
                gradleProperties.entries
                    .map { (k, v) -> "$k=$v" }
                    .sorted()
                    .joinToString(separator = "\n", postfix = "\n")
            )
        }

        private fun updateVersions(
            projectDir: Path,
            testedVersions: TestedVersions,
        ) {
            // Only update versions in specific files, to avoid reading and writing to lots of files.
            val namesOfFilesWithVersions = setOf(
                "build.gradle.kts",
                "settings.gradle.kts",
                "libs.versions.toml",
            )

            projectDir.walk()
                .filter { it.isRegularFile() && it.name in namesOfFilesWithVersions }
                .forEach { file ->
                    file.writeText(
                        file.readText()
                            .replace("/* %{KGP_VERSION} */", testedVersions.kgp.version)
                            .replace("/* %{DGP_VERSION} */", testedVersions.dgp.version)
                            .replace(
                                "/* %{AGP_VERSION} */",
                                if (testedVersions.hasAgp()) testedVersions.agp.version else "Missing AGP version"
                            )
                            .replace(
                                "/* %{COMPOSE_VERSION} */",
                                if (testedVersions.hasCompose()) testedVersions.composeGradlePlugin.version else "Missing Compose Gradle Plugin version"
                            )
                    )
                }
        }

        private fun initialiseAndroid(
            destination: Path,
        ) {
            destination.resolve("local.properties").apply {
                writeText(
                    """
                    |sdk.dir=${getAndroidSdkDir()}
                    |
                    """.trimMargin()
                )
            }
        }
    }
}
