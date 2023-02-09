package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.kotlin.dsl.get
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.kotlin.gistOf
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinSourceSetGistTest {

    @Test
    fun `main source set with kotlin jvm`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.jvm")
        val kotlin = project.kotlin as KotlinJvmProjectExtension

        val mainSourceSet = kotlin.sourceSets.getByName("main")
        val mainSourceSetGist = project.gistOf(mainSourceSet)

        assertEquals(
            "main", mainSourceSetGist.name,
            "Expected correct source set name"
        )

        assertEquals(
            KotlinPlatformType.jvm, mainSourceSetGist.platform.get(),
            "Expected correct platform"
        )

        assertTrue(
            mainSourceSetGist.isMain.get(),
            "Expected main sources to be marked as 'isMain'"
        )

        assertEquals(
            emptySet(), mainSourceSetGist.dependentSourceSetNames.get(),
            "Expected no dependent source sets"
        )
    }

    @Test
    fun `test source set with kotlin jvm`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.jvm")
        val kotlin = project.kotlin as KotlinJvmProjectExtension

        val testSourceSet = kotlin.sourceSets.getByName("test")
        val testSourceSetGist = project.gistOf(testSourceSet)

        assertFalse(
            testSourceSetGist.isMain.get(),
            "Expected test source set not being marked as 'isMain'"
        )

        assertEquals(
            emptySet(),
            testSourceSetGist.dependentSourceSetNames.get(),
            "Expected no dependent source sets"
        )
    }

    @Test
    fun `sourceRoots of main source set with kotlin jvm`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.jvm")
        val kotlin = project.kotlin as KotlinJvmProjectExtension
        val mainSourceSet = kotlin.sourceSets.getByName("main")
        val mainSourceSetGist = project.gistOf(mainSourceSet)

        assertEquals(
            emptySet(), mainSourceSetGist.sourceRoots.files,
            "Expected no sourceRoots, because default source root does not exist on filesystem yet"
        )

        // Create default source root on filesystem
        val defaultSourceRoot = project.file("src/main/kotlin")
        defaultSourceRoot.mkdirs()

        assertEquals(
            setOf(defaultSourceRoot), mainSourceSetGist.sourceRoots.files,
            "Expected default source root in source roots, since it is present on the filesystem"
        )

        // Create custom source set (and make sure it exists on filesystem)
        val customSourceRoot = project.file("src/main/custom").also(File::mkdirs)
        mainSourceSet.kotlin.srcDir(customSourceRoot)

        assertEquals(
            setOf(defaultSourceRoot, customSourceRoot), mainSourceSetGist.sourceRoots.files,
            "Expected recently registered custom source root to be present"
        )

        // removing default source root
        mainSourceSet.kotlin.setSrcDirs(listOf(customSourceRoot))

        assertEquals(
            setOf(customSourceRoot), mainSourceSetGist.sourceRoots.files,
            "Expected only custom source root being present in source roots"
        )
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun `classpath of main source set with kotlin jvm`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.jvm")
        val kotlin = project.kotlin as KotlinJvmProjectExtension
        val mainSourceSet = kotlin.sourceSets.getByName("main")
        val mainSourceSetGist = project.gistOf(mainSourceSet)

        /* Only work with file dependencies */
        project.configurations.forEach { configuration ->
            configuration.withDependencies {
                removeIf { dependency ->
                    dependency !is FileCollectionDependency
                }
            }
        }

        val implementationJar = project.file("implementation.jar")
        val compileOnlyJar = project.file("compileOnly.jar")
        val apiJar = project.file("api.jar")
        val runtimeOnlyJar = project.file("runtimeOnly.jar")


        mainSourceSet.dependencies {
            implementation(project.files(implementationJar))
            compileOnly(project.files(compileOnlyJar))
            api(project.files(apiJar))
            runtimeOnly(project.files(runtimeOnlyJar))
        }

        assertEquals(
            emptySet(), mainSourceSetGist.classpath.get().files,
            "Expected no files on the classpath, since no file exists"
        )

        /* Creating dependency files */
        check(implementationJar.createNewFile())
        check(compileOnlyJar.createNewFile())
        check(apiJar.createNewFile())
        check(runtimeOnlyJar.createNewFile())

        assertEquals(
            setOf(implementationJar, compileOnlyJar, apiJar), mainSourceSetGist.classpath.get().files,
            "Expected implementation, compileOnly and api dependencies on classpath"
        )
    }

    @Test
    fun `common, jvm and macos source sets with kotlin multiplatform`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        val kotlin = project.kotlin as KotlinMultiplatformExtension
        kotlin.jvm()
        kotlin.macosX64("macos")

        val commonMainSourceSet = kotlin.sourceSets.getByName("commonMain")
        val commonMainSourceSetGist = project.gistOf(commonMainSourceSet)

        val jvmMainSourceSet = kotlin.sourceSets.getByName("jvmMain")
        val jvmMainSourceSetGist = project.gistOf(jvmMainSourceSet)

        val macosMainSourceSet = kotlin.sourceSets.getByName("macosMain")
        val macosMainSourceSetGist = project.gistOf(macosMainSourceSet)

        assertEquals(
            "commonMain", commonMainSourceSetGist.name,
            "Expected correct source set name"
        )

        assertEquals(
            "jvmMain", jvmMainSourceSetGist.name,
            "Expected correct source set name"
        )

        assertEquals(
            "macosMain", macosMainSourceSetGist.name,
            "Expected correct source set name"
        )

        assertEquals(
            KotlinPlatformType.common, commonMainSourceSetGist.platform.get(),
            "Expected common platform for commonMain source set"
        )

        assertEquals(
            KotlinPlatformType.jvm, jvmMainSourceSetGist.platform.get(),
            "Expected jvm platform for jvmMain source set"
        )

        assertEquals(
            KotlinPlatformType.native, macosMainSourceSetGist.platform.get(),
            "Expected native platform for macosMain source set"
        )

        assertTrue(
            commonMainSourceSetGist.isMain.get(),
            "Expected commonMain to be marked with 'isMain'"
        )

        assertTrue(
            jvmMainSourceSetGist.isMain.get(),
            "Expected jvmMain to be marked with 'isMain'"
        )

        assertTrue(
            macosMainSourceSetGist.isMain.get(),
            "Expected macosMain to be marked with 'isMain'"
        )

        assertFalse(
            project.gistOf(kotlin.sourceSets["commonTest"]).isMain.get(),
            "Expected commonTest not being marked with 'isMain'"
        )

        assertFalse(
            project.gistOf(kotlin.sourceSets["jvmTest"]).isMain.get(),
            "Expected jvmTest not being marked with 'isMain'"
        )

        assertFalse(
            project.gistOf(kotlin.sourceSets["macosTest"]).isMain.get(),
            "Expected macosTest not being marked with 'isMain'"
        )

        assertEquals(
            setOf("commonMain"), jvmMainSourceSetGist.dependentSourceSetNames.get(),
            "Expected jvmMain to depend on commonMain by default"
        )

        /* Why not? */
        jvmMainSourceSet.dependsOn(macosMainSourceSet)
        assertEquals(
            setOf("commonMain", "macosMain"), jvmMainSourceSetGist.dependentSourceSetNames.get(),
            "Expected dependent source set changes to be reflected in gist"
        )
    }

}
