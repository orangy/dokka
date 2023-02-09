package org.jetbrains.dokka.gradle.utils

import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.utils.GradleProjectTest.Companion.testMavenRepoDir
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty


// utils for testing using Gradle TestKit


class GradleProjectTest(
    val projectDir: Path,
) {
    constructor(
        baseDir: Path = funcTestTempDir,
        projectPath: String = randomProjectPath(),
    ) : this(
        projectDir = baseDir.resolve(projectPath),
    )

    val runner: GradleRunner = GradleRunner.create().withProjectDir(projectDir.toFile())
    val testMavenRepoRelativePath: Path = projectDir.relativize(testMavenRepoDir)

    fun createFile(filePath: String, contents: String): File =
        projectDir.resolve(filePath).toFile().apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(contents)
        }

    companion object {

        /** file-based Maven Repo that contains the Dokka dependencies */
        val testMavenRepoDir: Path by lazy {
            Paths.get(
                System.getProperty("testMavenRepoDir") ?: error("testMavenRepoDir is unavailable")
            )
        }

        /** Temporary directory for the functional tests. This directory will be auto-deleted. */
        val funcTestTempDir: Path by lazy {
            Paths.get(
                System.getProperty("funcTestTempDir") ?: error("funcTestTempDir is unavailable")
            )
        }

        /** Directory that contains projects used for integration tests */
        val integrationTestProjectsDir: Path by lazy {
            Paths.get(
                System.getProperty("integrationTestProjectsDir") ?: error("integrationTestProjectsDir is unavailable")
            )
        }

//        val testTempDir = Files.createTempDirectory("dokka-tests")

//        fun tempDir(projectPath: String, baseDir: Path = funcTestTempDir): Path {
//            return when {
//                baseDir != null -> Paths.get(baseDir, projectPath)
//                else -> Files.createTempDirectory("dokka-test-$projectPath")
//            }
//        }

        fun randomProjectPath(): String = Random.nextInt(100_000_000, 999_999_999).toString()
    }
}


/**
 * Load a project from the [GradleProjectTest.integrationTestProjectsDir]
 */
fun gradleKtsProjectIntegrationTest(
    projectPath: String = GradleProjectTest.randomProjectPath(),
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest =
    GradleProjectTest(
        baseDir = GradleProjectTest.integrationTestProjectsDir,
        projectPath = projectPath,
    ).apply {
        build()
    }


/**
 * Builder for testing a Gradle project that uses Kotlin script DSL and creates default
 * `settings.gradle.kts` and `gradle.properties` files.
 *
 * @param[projectPath] the path of the project directory, relative to [baseDir
 */
fun gradleKtsProjectTest(
    projectPath: String = GradleProjectTest.randomProjectPath(),
    baseDir: Path = GradleProjectTest.funcTestTempDir,
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    return GradleProjectTest(baseDir = baseDir, projectPath = projectPath).apply {

        settingsGradleKts = """
            |rootProject.name = "test"
            |
            |@Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
            |dependencyResolutionManagement {
            |    repositories {
            |        mavenCentral()
            |        maven(file("$testMavenRepoRelativePath"))
            |    }
            |}
            |
            |pluginManagement {
            |    repositories {
            |        gradlePluginPortal()
            |        mavenCentral()
            |        maven(file("$testMavenRepoRelativePath"))
            |    }
            |}
            |
        """.trimMargin()

        gradleProperties = """
            |kotlin.mpp.stability.nowarn=true
            |org.gradle.cache=true
       """.trimMargin()

        build()
    }
}

/**
 * Builder for testing a Gradle project that uses Groovy script and creates default,
 * `settings.gradle`, and `gradle.properties` files.
 *
 * @param[projectPath] the path of the project directory, relative to [baseDir
 */
fun gradleGroovyProjectTest(
    baseDir: Path = GradleProjectTest.funcTestTempDir,
    projectPath: String = GradleProjectTest.randomProjectPath(),
    build: GradleProjectTest.() -> Unit,
): GradleProjectTest {
    return GradleProjectTest(baseDir = baseDir, projectPath = projectPath).apply {

        settingsGradle = """
            |rootProject.name = "test"
            |
            |dependencyResolutionManagement {
            |    repositories {
            |        mavenCentral()
            |        maven { url = file("$testMavenRepoRelativePath") }
            |    }
            |}
            |
            |pluginManagement {
            |    repositories {
            |        gradlePluginPortal()
            |        mavenCentral()
            |        maven { url = file("$testMavenRepoRelativePath") }
            |    }
            |}
            |
        """.trimMargin()

        gradleProperties = """
            |kotlin.mpp.stability.nowarn=true
            |org.gradle.cache=true
       """.trimMargin()

        build()
    }
}


/** Delegate for reading and writing a [GradleProjectTest] file. */
private class TestProjectFile(
    val filePath: String,
) : ReadWriteProperty<GradleProjectTest, String> {
    override fun getValue(thisRef: GradleProjectTest, property: KProperty<*>): String =
        thisRef.projectDir.resolve(filePath).toFile().readText()

    override fun setValue(thisRef: GradleProjectTest, property: KProperty<*>, value: String) {
        thisRef.createFile(filePath, value)
    }
}

/** Set the content of `settings.gradle.kts` */
@delegate:Language("kts")
var GradleProjectTest.settingsGradleKts: String by TestProjectFile("settings.gradle.kts")


/** Set the content of `build.gradle.kts` */
@delegate:Language("kts")
var GradleProjectTest.buildGradleKts: String by TestProjectFile("build.gradle.kts")


/** Set the content of `settings.gradle` */
@delegate:Language("groovy")
var GradleProjectTest.settingsGradle: String by TestProjectFile("settings.gradle")


/** Set the content of `build.gradle` */
@delegate:Language("groovy")
var GradleProjectTest.buildGradle: String by TestProjectFile("build.gradle")


/** Set the content of `gradle.properties` */
@delegate:Language("properties")
var GradleProjectTest.gradleProperties: String by TestProjectFile("gradle.properties")

fun GradleProjectTest.createKotlinFile(filePath: String, @Language("kotlin") contents: String) =
    createFile(filePath, contents)

fun GradleProjectTest.createKtsFile(filePath: String, @Language("kts") contents: String) =
    createFile(filePath, contents)
