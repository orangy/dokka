package org.jetbrains.dokka.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.*
import org.jetbrains.dokka.gradle.tasks.DokkaTask

class AndroidAutoConfigurationTest {

    private val project = ProjectBuilder.builder().build().also { project ->
        project.plugins.apply("com.android.library")
        project.plugins.apply("org.jetbrains.kotlin.android")
        project.plugins.apply("org.jetbrains.dokka")
        project.extensions.configure<LibraryExtension> {
            compileSdkVersion(28)
        }
    }

    @Test
    fun `at least one dokka task created`() {
        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        assertTrue(dokkaTasks.isNotEmpty(), "Expected at least one dokka task")
    }

    @Test
    fun `all default source sets are present in dokka`() {
        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        dokkaTasks.forEach { task ->
            val sourceSets = task.dokkaSourceSets.toList()
            assertEquals(
                listOf(
                    "androidTest", "androidTestDebug", "debug", "main",
                    "release", "test", "testDebug", "testRelease"
                ).sorted(),
                sourceSets.map { it.name }.sorted(),
                "Expected all default source sets being registered"
            )
        }
    }

    @Test
    fun `test source sets are suppressed`() {
        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        project as ProjectInternal
        project.evaluate()
        dokkaTasks.flatMap { it.dokkaSourceSets }.forEach { sourceSet ->
            if ("test" in sourceSet.name.toLowerCase()) {
                assertTrue(
                    sourceSet.suppress.get(),
                    "Expected source set `${sourceSet.name}` to be suppressed by default"
                )
            } else {
                assertFalse(
                    sourceSet.suppress.get(),
                    "Expected source set `${sourceSet.name}`to not be suppressed by default"
                )
            }
        }
    }

    @Test
    fun `source sets have non-empty classpath`() {
        val dokkaTasks = project.tasks.withType<DokkaTask>().toList()
        project as ProjectInternal
        project.evaluate()

        dokkaTasks.flatMap { it.dokkaSourceSets }.forEach { sourceSet ->
            /*

            There is no better way of checking for empty classpath at the moment (without resolving dependencies).
            We assume, that an empty classpath can be resolved
            We assume, that a non-empty classpath will not be able to resolve (no repositories defined)
             */

            assertFailsWith<ResolveException> { sourceSet.classpath.files }
        }
    }
}
