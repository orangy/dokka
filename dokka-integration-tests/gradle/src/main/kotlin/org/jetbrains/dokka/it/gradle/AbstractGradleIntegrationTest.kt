/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.GradleConnectionException
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.it.AbstractIntegrationTest
import java.io.File
import java.net.URI
import kotlin.test.BeforeTest
import kotlin.time.Duration.Companion.seconds

abstract class AbstractGradleIntegrationTest : AbstractIntegrationTest() {

    @BeforeTest
    fun copyTemplates() {
        File("projects").listFiles().orEmpty()
            .filter { it.isFile }
            .filter { it.name.startsWith("template.") }
            .forEach { file -> file.copyTo(File(tempFolder, file.name)) }
    }

    fun createGradleRunner(
        buildVersions: BuildVersions,
        vararg arguments: String,
    ): GradleRunner {
//        return GradleRunner(
//            projectDir = projectDir,
//        ) {
//            this.arguments += arguments
//        }
        return GradleRunner.create()
            .withProjectDir(projectDir)
//            .forwardOutput()
            .withJetBrainsCachedGradleVersion(buildVersions.gradleVersion)
            .withTestKitDir(File("build", "gradle-test-kit").absoluteFile)
            .withDebug(TestEnvironment.isEnabledDebug)
            .withArguments(
                listOfNotNull(
                    "-Porg.gradle.workers.max=1", // try to keep Gradle memory under control to prevent Metaspace OOMs (I'm not sure if this is effective!)
                    "-Pdokka_it_dokka_version=${System.getenv("DOKKA_VERSION")}",
                    "-Pdokka_it_kotlin_version=${buildVersions.kotlinVersion}",
                    buildVersions.androidGradlePluginVersion?.let { androidVersion ->
                        "-Pdokka_it_android_gradle_plugin_version=$androidVersion"
                    },

                    // Decrease Gradle daemon idle timeout, to help with OOM on CI because agents have limited memory
                    "-Dorg.gradle.daemon.idletimeout=" + 60.seconds.inWholeMilliseconds, // default is 3 hours!
                    "-Pkotlin.daemon.options.autoshutdownIdleSeconds=60",

                    // property flag to use K2
                    if (TestEnvironment.shouldUseK2())
                        "-P${TestEnvironment.TRY_K2}=true"
                    else
                        null,

                    * arguments
                )
            )
    }

    fun GradleRunner.buildRelaxed(): BuildResult {
        return try {
            build()
        } catch (e: Throwable) {
            val gradleConnectionException = e.withAllCauses().find { it is GradleConnectionException }
            if (gradleConnectionException != null) {
                println("Gradle Connection error!")
                gradleConnectionException.printStackTrace()
            }
//                gradleConnectionException.printStackTrace()
//                throw IllegalStateException("Assumed Gradle connection", e)
//            }
            throw e
        }
    }
}

private fun GradleRunner.withJetBrainsCachedGradleVersion(version: GradleVersion): GradleRunner =
    withGradleDistribution(
        URI.create(
            "https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-${version.version}-bin.zip"
        )
    )

private fun Throwable.withAllCauses(): Sequence<Throwable> {
    val root = this
    return sequence {
        yield(root)
        val cause = root.cause
        if (cause != null && cause != root) {
            yieldAll(cause.withAllCauses())
        }
    }
}
