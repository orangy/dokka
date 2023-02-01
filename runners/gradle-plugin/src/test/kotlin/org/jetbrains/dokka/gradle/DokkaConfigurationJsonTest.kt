package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.PluginConfigurationImpl
import org.jetbrains.dokka.toJsonString
import java.io.File
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals

class DokkaConfigurationJsonTest {

    @Test
    fun `DokkaTask configuration toJsonString then parseJson`() {
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
                PluginConfigurationImpl("A", DokkaConfiguration.SerializationFormat.JSON, """ { "key" : "value1" } """)
            )
            this.pluginsConfiguration.add(
                PluginConfigurationImpl("B", DokkaConfiguration.SerializationFormat.JSON, """ { "key" : "value2" } """)
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
        val configurationJson = sourceConfiguration.toJsonString()
        val parsedConfiguration = DokkaConfigurationImpl(configurationJson)

        assertEquals(sourceConfiguration, parsedConfiguration)
        println(parsedConfiguration)
    }
}
