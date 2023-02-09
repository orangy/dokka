package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.gradle.tooling.GradleConnectionException
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.junit.AssumptionViolatedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.net.URI
import kotlin.test.BeforeTest

@RunWith(Parameterized::class)
abstract class AbstractGradleIntegrationTest : AbstractIntegrationTest() {

    abstract val versions: BuildVersions

    @BeforeTest
    fun copyTemplates() {
        File("projects").listFiles().orEmpty()
            .filter { it.isFile }
            .filter { it.name.startsWith("template.") }
            .forEach { file -> file.copyTo(File(temporaryTestFolder.root, file.name)) }
    }

    fun createGradleRunner(
        vararg arguments: String,
        jvmArgs: List<String> = listOf("-Xmx4G", "-XX:MaxMetaspaceSize=2G")
    ): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withJetBrainsCachedGradleVersion(versions.gradleVersion)
            .withTestKitDir(File("build", "gradle-test-kit").absoluteFile)
            .withArguments(
                listOfNotNull(
                    "-Pkotlin_version=${versions.kotlinVersion}",
                    "-Pdokka_it_kotlin_version=${versions.kotlinVersion}",
                    versions.androidGradlePluginVersion?.let { androidVersion ->
                        "-Pdokka_it_android_gradle_plugin_version=$androidVersion"
                    },
                    *arguments
                )
            ).run { this as DefaultGradleRunner }
            .withJvmArguments(jvmArgs)
    }

    fun GradleRunner.buildRelaxed(): BuildResult {
        return try {
            build()
        } catch (e: Throwable) {
            val gradleConnectionException = e.withAllCauses().find { it is GradleConnectionException }
            if (gradleConnectionException != null) {
                gradleConnectionException.printStackTrace()
                throw AssumptionViolatedException("Assumed Gradle connection", gradleConnectionException)

            }
            throw e
        }
    }
}

private fun GradleRunner.withJetBrainsCachedGradleVersion(version: GradleVersion): GradleRunner {
    return withGradleDistribution(
        URI.create(
            "https://cache-redirector.jetbrains.com/" +
                    "services.gradle.org/distributions/" +
                    "gradle-${version.version}-bin.zip"
        )
    )
}

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
