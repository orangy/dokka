package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaDefaults.documentedVisibilities
import org.jetbrains.dokka.DokkaDefaults.includeNonPublic
import org.jetbrains.dokka.DokkaDefaults.reportUndocumented
import org.jetbrains.dokka.DokkaDefaults.skipDeprecated
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.dokka.gradle.tasks.DokkaTask

@Suppress("UnstableApiUsage")
class DokkaConfigurationSerializableTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `DokkaTask configuration write to file then parse`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")
        val dokkaTask = project.tasks.withType<DokkaTask>().first()
        dokkaTask.plugins.withDependencies { clear() }
        dokkaTask.apply {
            this.failOnWarning.set(true)
            this.offlineMode.set(true)
            this.outputDirectory.set(File("customOutputDir"))
            this.cacheRoot.set(File("customCacheRoot"))
            this.pluginsConfiguration.add(
                PluginConfigurationImpl(
                    "A",
                    DokkaConfiguration.SerializationFormat.JSON,
                    """ { "key" : "value1" } """
                )
            )
            this.pluginsConfiguration.add(
                PluginConfigurationImpl(
                    "B",
                    DokkaConfiguration.SerializationFormat.JSON,
                    """ { "key" : "value2" } """
                )
            )
            this.dokkaSourceSets.create("main") {
                displayName.set("customSourceSetDisplayName")
                reportUndocumented.set(true)

                externalDocumentationLink {
                    packageListUrl.set(URL("http://some.url"))
                    url.set(URL("http://some.other.url"))
                }

                perPackageOption {
                    includeNonPublic.set(true)
                    reportUndocumented.set(true)
                    skipDeprecated.set(true)
                    documentedVisibilities.set(setOf(DokkaConfiguration.Visibility.PRIVATE))
                }
            }
        }

        val sourceConfiguration = dokkaTask.buildDokkaConfiguration()
        val configurationFile = temporaryFolder.root.resolve("config.bin")
        ObjectOutputStream(configurationFile.outputStream()).use { stream ->
            stream.writeObject(sourceConfiguration)
        }
        val parsedConfiguration = ObjectInputStream(configurationFile.inputStream()).use { stream ->
            stream.readObject() as DokkaConfiguration
        }

        assertEquals(sourceConfiguration, parsedConfiguration)
    }
}
