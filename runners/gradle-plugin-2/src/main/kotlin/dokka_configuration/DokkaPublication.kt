package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_CONFIGURATIONS
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_CONFIGURATION_ELEMENTS
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_GENERATOR_CLASSPATH
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.TaskName.CREATE_DOKKA_CONFIGURATION
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.TaskName.CREATE_DOKKA_MODULE_CONFIGURATION
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.TaskName.DOKKA_GENERATE
import java.io.Serializable
import javax.inject.Inject

/**
 * A [DokkaPublication] describes a single Dokka output.
 *
 * Each Publication has its own set of Gradle tasks and [org.gradle.api.artifacts.Configuration]s.
 *
 * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
 * By default, Dokka will create publications for HTML, Jekyll, and GitHub Flavoured Markdown.
 */
abstract class DokkaPublication @Inject constructor(
    @get:Internal
    val formatName: String,
) : Named, Serializable {

    @Internal
    override fun getName(): String = formatName

    @get:Internal
    abstract val description: Property<String>

    @get:Input
    abstract val enabled: Property<Boolean>

    @Internal
    val taskNames = TaskNames()

    @Internal
    val configurationNames = ConfigurationNames()

    inner class TaskNames : Serializable {
        val generate: String = DOKKA_GENERATE + formatName.capitalize()
        val createConfiguration: String = CREATE_DOKKA_CONFIGURATION + formatName.capitalize()
        val createModuleConfiguration: String = CREATE_DOKKA_MODULE_CONFIGURATION + formatName.capitalize()
    }

    inner class ConfigurationNames : Serializable {
        val dokkaConfigurations: String = DOKKA_CONFIGURATIONS + formatName.capitalize()
        val dokkaConfigurationElements: String = DOKKA_CONFIGURATION_ELEMENTS + formatName.capitalize()
        val dokkaGeneratorClasspath: String = DOKKA_GENERATOR_CLASSPATH + formatName.capitalize()
        val dokkaPluginsClasspath: String = DOKKA_PLUGINS_CLASSPATH + formatName.capitalize()
        val dokkaPluginsIntransitiveClasspath: String = DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH + formatName.capitalize()
    }


    @get:Nested
    abstract val dokkaConfiguration: DokkaConfigurationGradleBuilder
}
