/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.LogLevel.*
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.GradleConnectionException
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.it.AbstractIntegrationTest
import org.jetbrains.dokka.it.optionalSystemProperty
import org.jetbrains.dokka.it.systemProperty
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.test.BeforeTest
import kotlin.time.Duration.Companion.seconds

abstract class AbstractGradleIntegrationTest : AbstractIntegrationTest() {

    @BeforeTest
    open fun beforeEachTest() {
        prepareProjectFiles()
    }

    fun prepareProjectFiles(
        templateProjectDir: Path = AbstractGradleIntegrationTest.templateProjectDir,
        destination: File = projectDir,
    ) {
        templateProjectDir.copyToRecursively(destination.toPath(), followLinks = false, overwrite = true)
        templateSettingsGradleKts.copyTo(destination.resolve("template.settings.gradle.kts").toPath(), overwrite = true)
        destination.toPath().updateProjectLocalMavenDir()
    }

    fun createGradleRunner(
        buildVersions: BuildVersions,
        vararg arguments: String,
        jvmArgs: List<String> = listOf("-Xmx2G", "-XX:MaxMetaspaceSize=1G"),
        enableBuildCache: Boolean? = true,
        /**
         * The log level that Gradle will use.
         *
         * Prefer using [LogLevel.LIFECYCLE] or above. Gradle TestKit stores logs in-memory, which makes the tests slow.
         * See https://github.com/gradle/gradle/issues/23965
         *
         * Avoid using [LogLevel.DEBUG] - it is *very* noisy!
         */
        gradleLogLevel: LogLevel = LIFECYCLE,
        /**
         * The log level that Dokka Generator will use.
         * Defaults to [gradleLogLevel], so that the Dokka logs are always produced.
         */
        dokkaLogLevel: LogLevel = gradleLogLevel,
    ): GradleRunner {

        // TODO quick hack to add `android { namespace }` on AGP 7+ (it's mandatory in 8+).
        //      This hack could be made prettier, or only test AGP 7+
        val androidMajorVersion = buildVersions.androidGradlePluginVersion
            ?.substringBefore(".")
            ?.toIntOrNull() ?: 0
        if (androidMajorVersion >= 7) {
            projectDir.resolve("build.gradle.kts").appendText(
                """
                |
                |android {
                |    namespace = "org.jetbrains.dokka.it.android"
                |}
                |
                """.trimMargin()
            )
        }

        return GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withJetBrainsCachedGradleVersion(buildVersions.gradleVersion)
            .withTestKitDir(File("build", "gradle-test-kit").absoluteFile)
            .withDebug(TestEnvironment.isEnabledDebug)
            .withReadOnlyDependencyCache()
            .withArguments(
                buildList {

                    when (gradleLogLevel) {
                        // For the 'LogLevel to cli-option' mapping, see https://docs.gradle.org/8.9/userguide/logging.html#sec:choosing_a_log_level
                        DEBUG -> add("--debug")
                        INFO -> add("--info")
                        LIFECYCLE -> {} // 'lifecycle' is the default and has no flag
                        WARN -> add("--warn")
                        QUIET -> add("--quiet")
                        ERROR -> add("--error")
                    }

                    add("-PdokkaGeneratorLogLevel=$dokkaLogLevel")

                    add("--stacktrace")

                    if (enableBuildCache != null) {
                        add(if (enableBuildCache) "--build-cache" else "--no-build-cache")
                    }

                    add("-Pdokka_it_dokka_version=${dokkaVersion}")
                    add("-Pdokka_it_kotlin_version=${buildVersions.kotlinVersion}")

                    buildVersions.androidGradlePluginVersion?.let { androidVersion ->
                        add("-Pdokka_it_android_gradle_plugin_version=$androidVersion")
                    }

                    // property flag to use K2
                    if (TestEnvironment.shouldUseK2()) {
                        add("-P${TestEnvironment.TRY_K2}=true")
                    }

                    // Decrease Gradle daemon idle timeout to prevent old agents lingering on CI.
                    // A lower timeout means slower tests, which is preferred over OOMs and locked processes.
                    add("-Dorg.gradle.daemon.idletimeout=" + 10.seconds.inWholeMilliseconds) // default is 3 hours!
                    add("-Pkotlin.daemon.options.autoshutdownIdleSeconds=10")
                    addAll(arguments)
                }
            )
            .withJvmArguments(jvmArgs)
    }

    fun GradleRunner.buildRelaxed(): BuildResult {
        return try {
            build()
        } catch (e: Throwable) {
            val gradleConnectionException = e.withAllCauses().find { it is GradleConnectionException }
            if (gradleConnectionException != null) {
                gradleConnectionException.printStackTrace()
                throw IllegalStateException("Assumed Gradle connection", gradleConnectionException)
            }
            throw e
        }
    }

    companion object {
        private val dokkaVersionOverride: String? by optionalSystemProperty()
        private val dokkaVersion: String by systemProperty { dokkaVersionOverride ?: it }

        /**
         * Location of the template project that will be copied into [AbstractIntegrationTest.projectDir].
         *
         * The contents of this directory _must not_ be modified.
         *
         * The value is provided by the Gradle Test task.
         */
        val templateProjectDir: Path by systemProperty(Paths::get)

        /**
         * Location of the `template.settings.gradle.kts` file used to provide common Gradle Settings configuration for template projects.
         *
         * This value is provided by the Gradle Test task.
         */
        val templateSettingsGradleKts: Path by systemProperty(Paths::get)

        /**
         * Gradle User Home of the current machine. Defaults to `~/.gradle`, but might be different on CI.
         *
         * This value is provided by the Gradle Test task.
         */
        private val hostGradleUserHome: Path by systemProperty(Paths::get)

        /**
         * Gradle dependencies cache of the current machine.
         *
         * Used as a read-only dependencies cache by setting `GRADLE_RO_DEP_CACHE`
         *
         * See https://docs.gradle.org/8.9/userguide/dependency_resolution.html#sub:cache_copy
         *
         * Note: Currently all Gradle versions store caches in `$GRADLE_USER_HOME/caches/`,
         * but this might change. Check the docs.
         */
        internal val hostGradleDependenciesCache: Path by lazy {
            hostGradleUserHome.resolve("caches")
        }

        /** file-based Maven repositories with Dokka dependencies */
        private val devMavenRepositories: List<Path> by systemProperty { repos ->
            repos.split(",").map { Paths.get(it) }
        }

        private val mavenRepositories: String by lazy {
            val reposSpecs = if (dokkaVersionOverride != null) {
                println("Dokka version overridden with $dokkaVersionOverride")
                // if `DOKKA_VERSION_OVERRIDE` environment variable is provided,
                //  we allow running tests on a custom Dokka version from specific repositories
                """
                maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test"),
                maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev"),
                mavenCentral(),
                mavenLocal()
                """.trimIndent()
            } else {
                // otherwise - use locally published versions via `devMavenPublish`
                devMavenRepositories.withIndex().joinToString(",\n") { (i, repoPath) ->
                    // Exclusive repository containing local Dokka artifacts.
                    // Must be compatible with both Groovy and Kotlin DSL.
                    /* language=kts */
                    """
                    |maven {
                    |    setUrl("${repoPath.invariantSeparatorsPathString}")
                    |    name = "DokkaDevMavenRepo${i}"
                    |}
                    """.trimMargin()
                }
            }

            /* language=kts */
            """
            |exclusiveContent {
            |    forRepositories(
            |      $reposSpecs
            |    )
            |    filter {
            |        includeGroup("org.jetbrains.dokka")
            |    }
            |}
            |
            """.trimMargin()
        }

        fun Path.updateProjectLocalMavenDir() {

            val dokkaMavenRepoMarker = "/* %{DOKKA_IT_MAVEN_REPO}% */"

            // Exclusive repository containing local Dokka artifacts.
            // Must be compatible with both Groovy and Kotlin DSL.

            walk().filter { it.isRegularFile() }.forEach { file ->
                val fileText = file.readText()

                if (dokkaMavenRepoMarker in fileText) {
                    file.writeText(
                        fileText.replace(dokkaMavenRepoMarker, mavenRepositories)
                    )
                }
            }
        }
    }
}

private fun GradleRunner.withJetBrainsCachedGradleVersion(version: GradleVersion): GradleRunner =
    withJetBrainsCachedGradleVersion(version.version)

internal fun GradleRunner.withJetBrainsCachedGradleVersion(version: String): GradleRunner =
    withGradleDistribution(
        URI("https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-${version}-bin.zip")
    )

internal fun GradleRunner.withReadOnlyDependencyCache(
    hostGradleDependenciesCache: Path = AbstractGradleIntegrationTest.hostGradleDependenciesCache,
): GradleRunner =
    apply {
        withEnvironment(
            buildMap {
                // `withEnvironment()` will wipe all existing environment variables,
                // which breaks things like ANDROID_HOME and PATH, so re-add them.
                putAll(System.getenv())

                if (hostGradleDependenciesCache.exists()) {
                    put("GRADLE_RO_DEP_CACHE", hostGradleDependenciesCache.invariantSeparatorsPathString)
                }
            }
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
