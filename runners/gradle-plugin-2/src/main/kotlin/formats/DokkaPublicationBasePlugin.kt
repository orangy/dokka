package org.jetbrains.dokka.gradle.formats

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaPublication
import javax.inject.Inject

/**
 * Base Gradle Plugin for setting up a Dokka Publication for a specific format.
 *
 * [DokkaPlugin] must be applied for this plugin, or any implementation, to have an effect.
 */
abstract class DokkaPublicationBasePlugin @Inject constructor(
    val formatName: String,
) : Plugin<Project> {

    final override fun apply(target: Project) {
        // TODO maybe auto apply DokkaPlugin? Then these plugins can be used independently

        target.plugins.withType<DokkaPlugin>().configureEach {
            val dokkaExtension = target.extensions.getByType(DokkaExtension::class)

            val publication = dokkaExtension.dokkaPublications.create(formatName)

            val context = PublicationPluginContext(target, dokkaExtension, publication)

            context.configure()
        }
    }

    class PublicationPluginContext(
        val project: Project,
        val dokkaExtension: DokkaExtension,
        val publication: DokkaPublication,
    ) {

        fun DependencyHandler.dokka(module: String) =
            dokkaExtension.dokkaVersion.map { version -> create("org.jetbrains.dokka:$module:$version") }

        fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>) =
            addProvider(publication.configurationNames.dokkaPluginsClasspath, dependency)

        fun DependencyHandler.dokkaPlugin(dependency: String) =
            add(publication.configurationNames.dokkaPluginsClasspath, dependency)

        fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) =
            addProvider(publication.configurationNames.dokkaGeneratorClasspath, dependency)

        fun DependencyHandler.dokkaGenerator(dependency: String) =
            add(publication.configurationNames.dokkaGeneratorClasspath, dependency)
    }

    /** Format specific configuration */
    open fun PublicationPluginContext.configure() {}
}
