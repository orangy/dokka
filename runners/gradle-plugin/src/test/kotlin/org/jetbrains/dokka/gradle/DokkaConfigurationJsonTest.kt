package org.jetbrains.dokka.gradle

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaDefaults.documentedVisibilities
import org.jetbrains.dokka.DokkaDefaults.includeNonPublic
import org.jetbrains.dokka.DokkaDefaults.reportUndocumented
import org.jetbrains.dokka.DokkaDefaults.skipDeprecated
import org.jetbrains.dokka.gradle.tasks.DokkaTask
import org.jetbrains.dokka.gradle.util.create_
import org.jetbrains.dokka.gradle.util.externalDocumentationLink_
import org.jetbrains.dokka.gradle.util.withDependencies_
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
        dokkaTask.plugins.withDependencies_ { clear() }
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
            this.dokkaSourceSets.create_("main") {
                displayName.set("customSourceSetDisplayName")
                reportUndocumented.set(true)

                externalDocumentationLink_ {
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
