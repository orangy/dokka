/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.utils.GradleProjectTest.Companion.settingsRepositories
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


// utils for testing using Gradle TestKit


class GradleProjectTest(
    override val projectDir: Path,
) : ProjectDirectoryScope {

    val gradleProperties = GradlePropertiesBuilder()

    fun gradleProperties(config: GradlePropertiesBuilder.() -> Unit) {
        gradleProperties.config()
    }

    constructor(
        testProjectName: String,
        baseDir: Path = funcTestTempDir,
    ) : this(projectDir = baseDir.resolve(testProjectName))

    val runner: GradleRunner
        get() = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .writeGradleProperties(gradleProperties)

    /**
     * Update all repositories in any `settings.gradle(.kts)` file.
     *
     * All `repositories {}` will be modified to add the dev maven repos, [mavenRepositories].
     */
    fun updateSettingsRepositories() {
        projectDir.walk()
            .filter { it.name == "settings.gradle.kts" || it.name == "settings.gradle" }
            .forEach { settingsGradle ->
                val repoLine = settingsGradle.useLines { it.firstOrNull { l -> l.trim() == "repositories {" } }
                    ?: return@forEach
                val ind = repoLine.substringBefore("repositories {")
                settingsGradle.writeText(
                    settingsGradle.readText().replace(
                        "repositories {",
                        "repositories {\n${mavenRepositories.prependIndent(ind)}\n",
                    )
                )
            }
    }

    companion object {
        private val dokkaVersionOverride: String? by optionalSystemProperty()

        /** file-based Maven repositories with Dokka dependencies */
        private val devMavenRepositories: List<Path> by systemProperty { repos ->
            repos.split(",").map { Path(it) }
        }

        private val projectTestTempDir: Path by systemProperty(Paths::get)

        /** Temporary directory for the functional tests */
        val funcTestTempDir: Path by lazy {
            projectTestTempDir.resolve("functional-tests")
        }

        fun settingsRepositories(): String {
            val dokkaTestRepo = mavenRepositories

            return """
                |pluginManagement {
                |  repositories {
                |${dokkaTestRepo.prependIndent("    ")}
                |    mavenCentral()
                |    gradlePluginPortal()
                |  }
                |}
                |
                |@Suppress("UnstableApiUsage")
                |dependencyResolutionManagement {
                |  repositories {
                |${dokkaTestRepo.prependIndent("    ")}
                |    mavenCentral()
                |  }
                |}
                |
              """.trimMargin()
        }


        val mavenRepositories: String by lazy {
            val reposSpecs = if (dokkaVersionOverride != null) {
                println("Dokka version overridden with $dokkaVersionOverride")
                // if `DOKKA_VERSION_OVERRIDE` environment variable is provided,
                // we allow running tests on a custom Dokka version from specific repositories
                """
                maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test"),
                maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev"),
                mavenCentral(),
                mavenLocal()
                """.trimIndent()
            } else {
                // otherwise - use locally published versions via `devMavenPublish`
                GradleProjectTest.devMavenRepositories.withIndex().joinToString(",\n") { (i, repoPath) ->
                    // Exclusive repository containing local Dokka artifacts.
                    // Must be compatible with both Groovy and Kotlin DSL.
                    """
                    |maven {
                    |    setUrl("${repoPath.invariantSeparatorsPathString}")
                    |    name = "DokkaDevMavenRepo${i}"
                    |}
                    """.trimMargin()
                }
            }

            """
            |exclusiveContent {
            |    forRepositories(
            |      $reposSpecs
            |    )
            |    filter {
            |        includeGroup("org.jetbrains.dokka")
            |        includeGroup("org.jetbrains.dokka-javadoc")
            |    }
            |}
            |
            """.trimMargin()
        }
    }
}


/**
 * Builder for testing a Gradle project that uses Kotlin script DSL and creates default
 * `settings.gradle.kts` and `gradle.properties` files.
 *
 * @param[projectLocation] the path of the project directory, relative to [baseDir]
 * @param[rootProjectName] the name of the Gradle project, configured in the `settings.gradle.kts`
 */
fun gradleKtsProjectTest(
    projectLocation: String,
    rootProjectName: String? = null,
    baseDir: Path = GradleProjectTest.funcTestTempDir,
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {

    val rootProjectNameValue: String = rootProjectName
        ?: projectLocation.removeSuffix("/").substringAfterLast('/')

    return gradleProjectTest(
        testProjectName = rootProjectNameValue,
        baseDir = baseDir.resolve(projectLocation),
    ) {

        settingsGradleKts = """
            |rootProject.name = "$rootProjectNameValue"
            |
            |${settingsRepositories()}
            |
            """.trimMargin()

        build()
    }
}

/**
 * Builder for testing a Gradle project that uses Groovy script and creates default,
 * `settings.gradle`, and `gradle.properties` files.
 *
 * @param[projectLocation] the path of the project directory, relative to [baseDir]
 * @param[rootProjectName] the name of the Gradle project, configured in the `settings.gradle`
 */
fun gradleGroovyProjectTest(
    projectLocation: String,
    rootProjectName: String? = null,
    baseDir: Path = GradleProjectTest.funcTestTempDir,
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    val rootProjectNameValue: String = rootProjectName
        ?: projectLocation.removeSuffix("/").substringAfterLast('/')

    return gradleProjectTest(
        testProjectName = rootProjectNameValue,
        baseDir = baseDir,
    ) {
        settingsGradle = """
            |rootProject.name = "$rootProjectNameValue"
            |
            |${settingsRepositories()}
            |
            """.trimMargin()

        build()
    }
}

private fun gradleProjectTest(
    testProjectName: String,
    baseDir: Path = GradleProjectTest.funcTestTempDir,
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    return GradleProjectTest(baseDir = baseDir, testProjectName = testProjectName).apply {
        build()
    }
}


fun GradleProjectTest.projectFile(
    @Language("TEXT")
    filePath: String
): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, String>> =
    PropertyDelegateProvider { _, _ ->
        TestProjectFileProvidedDelegate(this, filePath)
    }


/** Delegate for reading and writing a [GradleProjectTest] file. */
private class TestProjectFileProvidedDelegate(
    private val project: GradleProjectTest,
    private val filePath: String,
) : ReadWriteProperty<Any?, String> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): String =
        project.projectDir.resolve(filePath).readText()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        project.createFile(filePath, value)
    }
}

/** Delegate for reading and writing a [GradleProjectTest] file. */
class TestProjectFileDelegate(
    private val filePath: String,
) : ReadWriteProperty<ProjectDirectoryScope, String> {
    override fun getValue(thisRef: ProjectDirectoryScope, property: KProperty<*>): String =
        thisRef.projectDir.resolve(filePath).readText()

    override fun setValue(thisRef: ProjectDirectoryScope, property: KProperty<*>, value: String) {
        thisRef.createFile(filePath, value)
    }
}


@DslMarker
annotation class ProjectDirectoryDsl

@ProjectDirectoryDsl
interface ProjectDirectoryScope {
    val projectDir: Path
}

private data class ProjectDirectoryScopeImpl(
    override val projectDir: Path
) : ProjectDirectoryScope


fun ProjectDirectoryScope.createFile(filePath: String, contents: String): File =
    projectDir.resolve(filePath).toFile().apply {
        parentFile.mkdirs()
        createNewFile()
        writeText(contents)
    }


@ProjectDirectoryDsl
fun ProjectDirectoryScope.dir(
    path: String,
    block: ProjectDirectoryScope.() -> Unit = {},
): ProjectDirectoryScope =
    ProjectDirectoryScopeImpl(projectDir.resolve(path)).apply(block)


@ProjectDirectoryDsl
fun ProjectDirectoryScope.file(
    path: String
): Path = projectDir.resolve(path)


fun ProjectDirectoryScope.findFiles(matcher: (Path) -> Boolean): Sequence<Path> =
    projectDir.walk().filter(matcher)


/** Set the content of `settings.gradle.kts` */
@delegate:Language("kts")
var ProjectDirectoryScope.settingsGradleKts: String by TestProjectFileDelegate("settings.gradle.kts")


/** Set the content of `build.gradle.kts` */
@delegate:Language("kts")
var ProjectDirectoryScope.buildGradleKts: String by TestProjectFileDelegate("build.gradle.kts")


/** Set the content of `settings.gradle` */
@delegate:Language("groovy")
var ProjectDirectoryScope.settingsGradle: String by TestProjectFileDelegate("settings.gradle")


/** Set the content of `build.gradle` */
@delegate:Language("groovy")
var ProjectDirectoryScope.buildGradle: String by TestProjectFileDelegate("build.gradle")

fun ProjectDirectoryScope.createKotlinFile(
    filePath: String,
    @Language("kotlin") contents: String
): File =
    createFile(filePath, contents)


fun ProjectDirectoryScope.createKtsFile(
    filePath: String,
    @Language("kts") contents: String
): File =
    createFile(filePath, contents)
