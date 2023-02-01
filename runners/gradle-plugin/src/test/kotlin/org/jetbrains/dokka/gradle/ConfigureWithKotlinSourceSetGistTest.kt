package org.jetbrains.dokka.gradle

import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.kotlin.dsl.get
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.kotlin.KotlinSourceSetGist
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigureWithKotlinSourceSetGistTest {
    @Test
    fun `example gist`() {
        val project = ProjectBuilder.builder().build()

        val f1Jar = project.file("f1.jar")
        val f2Jar = project.file("f2.jar")
        check(f1Jar.createNewFile())
        check(f2Jar.createNewFile())

        val customSourceRoot = project.file("customSourceRoot")
        check(customSourceRoot.mkdirs())

        val gist = KotlinSourceSetGist(
            name = "customName",
            platform = project.provider { KotlinPlatformType.common },
            isMain = project.provider { true },
            classpath = project.provider { project.files(f1Jar, f2Jar) },
            sourceRoots = project.files(customSourceRoot),
            dependentSourceSetNames = project.provider { setOf("customRootSourceSet") }
        )

        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        sourceSet.configureWithKotlinSourceSetGist(gist)

        assertEquals(
            "common", sourceSet.build().displayName,
            "Expected platform being used as default displayName for source set"
        )

        assertEquals(
            Platform.common, sourceSet.build().analysisPlatform,
            "Expected common platform being set"
        )

        assertEquals(
            listOf(f1Jar, f2Jar), sourceSet.build().classpath,
            "Expected classpath being present"
        )

        assertEquals(
            setOf(sourceSet.DokkaSourceSetID("customRootSourceSet")), sourceSet.build().dependentSourceSets,
            "Expected customRootSourceSet being present in dependentSourceSets after build"
        )

        assertEquals(
            setOf(customSourceRoot), sourceSet.build().sourceRoots,
            "Expected customSourceRoot being present in sourceRoots after build"
        )
    }

    @Test
    fun `display name for source set customMain`() {
        val project = ProjectBuilder.builder().build()

        val gist = KotlinSourceSetGist(
            name = "customMain",
            platform = project.provider { KotlinPlatformType.common },
            isMain = project.provider { true },
            classpath = project.provider { project.files() },
            sourceRoots = project.files(),
            dependentSourceSetNames = project.provider { emptySet() }
        )

        val sourceSet = GradleDokkaSourceSetBuilder("", project)
        sourceSet.configureWithKotlinSourceSetGist(gist)

        assertEquals(
            "custom", sourceSet.build().displayName,
            "Expected 'Main' being trimmed from source set name and used as display name"
        )
    }

    @Suppress("UnstableApiUsage")
    @Test
    fun `configuration with kotlin source set is live`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.jvm")
        val kotlin = project.kotlin as KotlinJvmProjectExtension
        val mainSourceSet = kotlin.sourceSets["main"]

        /* Make sure that the source roots exist on filesystem */
        mainSourceSet.kotlin.sourceDirectories.elements.get().map { it.asFile }.forEach { it.mkdirs() }

        /* Make sure to remove dependencies that cannot be resolved during test */
        project.configurations.configureEach {
            withDependencies {
                removeIf { dependency -> dependency !is FileCollectionDependency }
            }
        }

        val dokkaSourceSet = GradleDokkaSourceSetBuilder("main", project)
        dokkaSourceSet.kotlinSourceSet(mainSourceSet)

        assertEquals(
            listOf(project.file("src/main/kotlin"), project.file("src/main/java")),
            dokkaSourceSet.sourceRoots.elements.get().map { it.asFile },
            "Expected default source roots being present in dokkaSourceSet"
        )

        val customSourceRoot = project.file("src/main/customRoot")
        check(customSourceRoot.mkdirs())
        mainSourceSet.kotlin.srcDir(customSourceRoot)

        assertEquals(
            listOf(project.file("src/main/kotlin"), project.file("src/main/java"), project.file("src/main/customRoot")),
            dokkaSourceSet.sourceRoots.elements.get().map { it.asFile },
            "Expected customRoot being added to source roots in dokkaSourceSet"
        )
    }

    @Test
    fun `changing classpath`() {
        val project = ProjectBuilder.builder().build()
        val dokkaSourceSet = GradleDokkaSourceSetBuilder("main", project)
        var classpath = project.files()

        dokkaSourceSet.configureWithKotlinSourceSetGist(
            KotlinSourceSetGist(
                name = "gist",
                platform = project.provider { KotlinPlatformType.common },
                isMain = project.provider { true },
                dependentSourceSetNames = project.provider { emptySet() },
                sourceRoots = project.files(),
                classpath = project.provider { classpath }
            )
        )

        dokkaSourceSet.classpath.from("base.jar")
        classpath.from("f1.jar")
        classpath.from("f2.jar")
        assertEquals(
            setOf(project.file("f1.jar"), project.file("f2.jar"), project.file("base.jar")),
            dokkaSourceSet.classpath.files,
            "Expected files from initial gist classpath and manually added file base.jar to be present in classpath"
        )

        /*
        Swapping the original file collection in favour of a new one.
        We expect that the base.jar is still present, as it was configured on the dokka source set.
        We also expect, that the new files from the new file collection are replacing old ones
         */
        classpath = project.files("f3.jar", "f4.jar")
        assertEquals(
            setOf(project.file("f3.jar"), project.file("f4.jar"), project.file("base.jar")),
            dokkaSourceSet.classpath.files,
            "Expected files from changed gist classpath and manually added file base.jar to be present in classpath"
        )
    }


}
