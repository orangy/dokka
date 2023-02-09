package org.jetbrains.dokka.gradle.tasks

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaDefaults.cacheRoot
import org.jetbrains.dokka.DokkaDefaults.failOnWarning
import org.jetbrains.dokka.DokkaDefaults.moduleName
import org.jetbrains.dokka.DokkaDefaults.offlineMode
import org.jetbrains.dokka.DokkaException
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.jetbrains.dokka.gradle.tasks.*
import org.jetbrains.dokka.gradle.*

class DokkaCollectorTaskTest {

    @Test
    fun buildDokkaConfiguration() {
        val rootProject = ProjectBuilder.builder().build()
        val childProject = ProjectBuilder.builder().withParent(rootProject).build()
        childProject.plugins.apply("org.jetbrains.kotlin.jvm")

        rootProject.allprojects {
            plugins.apply("org.jetbrains.dokka")
            tasks.withType<AbstractDokkaTask>().configureEach {
                plugins.withDependencies { clear() }
            }
            tasks.withType<DokkaTask>().configureEach {
                dokkaSourceSets.configureEach {
                    classpath.setFrom(emptyList<Any>())
                }
            }
        }

        val collectorTasks = rootProject.tasks.withType<DokkaCollectorTask>()
        collectorTasks.configureEach {
            moduleName.set("custom Module Name")
            outputDirectory.set(File("customOutputDirectory"))
            cacheRoot.set(File("customCacheRoot"))
            failOnWarning.set(true)
            offlineMode.set(true)
            finalizeCoroutines.set(false)
        }

        assertTrue(collectorTasks.isNotEmpty(), "Expected at least one collector task")

        collectorTasks.toList().forEach { task ->
            val dokkaConfiguration = task.buildDokkaConfiguration()
            assertEquals(
                DokkaConfigurationImpl(
                    moduleName = "custom Module Name",
                    outputDir = File("customOutputDirectory"),
                    cacheRoot = File("customCacheRoot"),
                    failOnWarning = true,
                    offlineMode = true,
                    sourceSets = task.childDokkaTasks
                        .map { it.buildDokkaConfiguration() }
                        .map { it.sourceSets }
                        .reduce { acc, list -> acc + list },
                    pluginsClasspath = task.childDokkaTasks
                        .map { it.plugins.resolve().toList() }
                        .reduce { acc, mutableSet -> acc + mutableSet },
                    finalizeCoroutines = false,
                ),
                dokkaConfiguration
            )
        }
    }

    @Test
    fun `with no child tasks throws DokkaException`() {
        val project = ProjectBuilder.builder().build()
        val collectorTask = project.tasks.create<DokkaCollectorTask>("collector")
        project.configurations.all { withDependencies { clear() } }
        assertFailsWith<DokkaException> { collectorTask.generateDocumentation() }
    }
}
