package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.gradle.tasks.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinDslDokkaTaskConfigurationTest {
    @Test
    fun `configure dokka task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        project.tasks.withType<DokkaTask>().configureEach {
            outputDirectory.set(File("test"))
        }

        project.tasks.withType(DokkaTask::class.java).forEach { dokkaTask ->
            assertEquals(File("test"), dokkaTask.outputDirectory.get())
        }
    }

    @Test
    fun `sourceSet dependsOn by String`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        project.tasks.withType(DokkaTask::class.java).forEach { dokkaTask ->
            dokkaTask.dokkaSourceSets.run {
                val commonMain = create("commonMain")
                val jvmMain = create("jvmMain") {
                    dependsOn("commonMain")
                }

                assertEquals(
                    0, commonMain.dependentSourceSets.get().size,
                    "Expected no dependent source set in commonMain"
                )

                assertEquals(
                    1, jvmMain.dependentSourceSets.get().size,
                    "Expected only one dependent source set in jvmMain"
                )

                assertEquals(
                    commonMain.sourceSetID, jvmMain.dependentSourceSets.get().single(),
                    "Expected jvmMain to depend on commonMain"
                )

                assertEquals(
                    DokkaSourceSetID(dokkaTask, "commonMain"), commonMain.sourceSetID
                )
            }
        }
    }

    @Test
    fun `sourceSet dependsOn by DokkaSourceSet`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        project.tasks.withType(DokkaTask::class.java).first().run {
            dokkaSourceSets.run {
                val commonMain = create("commonMain")
                val jvmMain = create("jvmMain") {
                    dependsOn(commonMain)
                }

                assertEquals(
                    commonMain.sourceSetID, jvmMain.dependentSourceSets.get().single()
                )
            }
        }
    }

    @Test
    fun `sourceSet dependsOn by KotlinSourceSet`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        project.plugins.apply("org.jetbrains.kotlin.jvm")

        val kotlin = project.extensions.getByName("kotlin") as KotlinJvmProjectExtension

        project.tasks.withType(DokkaTask::class.java).first().apply {
            dokkaSourceSets.run {
                val special = create("special") {
                    dependsOn(kotlin.sourceSets.getByName("main"))
                }

                assertEquals(
                    DokkaSourceSetID(this@apply, "main"), special.dependentSourceSets.get().single()
                )
            }
        }
    }
}
