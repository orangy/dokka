/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.formats

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.adapters.AndroidAdapter
import org.jetbrains.dokka.gradle.adapters.JavaAdapter
import org.jetbrains.dokka.gradle.adapters.KotlinAdapter
import org.jetbrains.dokka.gradle.dependencies.DependencyContainerNames
import org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.Companion.DokkaClasspathAttribute
import org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.Companion.DokkaFormatAttribute
import org.jetbrains.dokka.gradle.dependencies.FormatDependenciesManager
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.PluginFeaturesService.Companion.pluginFeaturesService
import javax.inject.Inject

/**
 * Base Gradle Plugin for setting up a Dokka Publication for a specific output format.
 *
 * [DokkaBasePlugin] must be applied for this plugin (or any subclass) to have an effect.
 *
 * Anyone can use this class as a basis for a generating a Dokka Publication in a custom format.
 */
abstract class DokkaFormatPlugin(
    val formatName: String,
) : Plugin<Project> {

    @get:Inject
    @InternalDokkaGradlePluginApi
    protected abstract val objects: ObjectFactory

    @get:Inject
    @InternalDokkaGradlePluginApi
    protected abstract val providers: ProviderFactory

    @get:Inject
    @InternalDokkaGradlePluginApi
    protected abstract val files: FileSystemOperations

    @get:Inject
    @InternalDokkaGradlePluginApi
    protected abstract val layout: ProjectLayout


    override fun apply(target: Project) {

        // apply DokkaBasePlugin
        target.pluginManager.apply(DokkaBasePlugin::class)

        // apply the plugin that will autoconfigure Dokka to use the sources of a Kotlin project
        target.pluginManager.apply(type = KotlinAdapter::class)
        target.pluginManager.apply(type = JavaAdapter::class)
        target.pluginManager.apply(type = AndroidAdapter::class)

        target.plugins.withType<DokkaBasePlugin>().configureEach {
            val dokkaExtension = target.extensions.getByType(DokkaExtension::class)

            val publication = dokkaExtension.dokkaPublications.create(formatName)

            val formatDependencies = FormatDependenciesManager(
                project = target,
                baseDependencyManager = dokkaExtension.baseDependencyManager,
                formatName = formatName,
                objects = objects,
            )

            val dokkaTasks = DokkaFormatTasks(
                project = target,
                publication = publication,
                formatDependencies = formatDependencies,
                providers = providers,
            )

            formatDependencies.moduleOutputDirectories
                .outgoing
                .outgoing
                .artifact(dokkaTasks.generateModule.map { it.outputDirectory }) {
                    builtBy(dokkaTasks.generateModule)
                    type = "dokka-module-directory"
                }

            dokkaTasks.generatePublication.configure {
                generator.moduleOutputDirectories.from(
                    formatDependencies.moduleOutputDirectories.incomingArtifactFiles
                )
                generator.pluginsClasspath.from(
                    formatDependencies.dokkaPublicationPluginClasspathResolver
                )
            }

            val context = DokkaFormatPluginContext(
                project = target,
                dokkaExtension = dokkaExtension,
                dokkaTasks = dokkaTasks,
                formatDependencies = formatDependencies,
                formatName = formatName,
            )

            context.configure()

            if (context.addDefaultDokkaDependencies) {
                with(context) {
                    addDefaultDokkaDependencies()
                }
            }

            if (context.enableVersionAlignment) {
                //region version alignment
                listOf(
                    formatDependencies.dokkaPluginsIntransitiveClasspathResolver,
                    formatDependencies.dokkaGeneratorClasspathResolver,
                ).forEach { dependenciesContainer: Configuration ->
                    // Add a version if one is missing, which will allow defining a org.jetbrains.dokka
                    // dependency without a version.
                    // (It would be nice to do this with a virtual-platform, but Gradle is bugged:
                    // https://github.com/gradle/gradle/issues/27435)
                    dependenciesContainer.resolutionStrategy.eachDependency {
                        if (requested.group == "org.jetbrains.dokka" && requested.version.isNullOrBlank()) {
                            val dokkaVersion = dokkaExtension.dokkaEngineVersion.get()
                            logger.info("[${context.project.path}] adding Dokka version $dokkaVersion to dependency '$requested'")
                            useVersion(dokkaVersion)
                        }
                    }
                }
                //endregion
            }
        }
    }

    /** Format specific configuration - to be implemented by subclasses */
    open fun DokkaFormatPluginContext.configure() {}


    @InternalDokkaGradlePluginApi
    class DokkaFormatPluginContext(
        val project: Project,
        val dokkaExtension: DokkaExtension,
        val dokkaTasks: DokkaFormatTasks,
        val formatDependencies: FormatDependenciesManager,
        formatName: String,
    ) {
        private val dependencyContainerNames: DependencyContainerNames =
            DependencyContainerNames(formatName)

        var addDefaultDokkaDependencies: Boolean = true
        var enableVersionAlignment: Boolean = true

        /** Create a [Dependency] for a Dokka module */
        fun DependencyHandler.dokka(module: String): Provider<Dependency> =
            dokkaExtension.dokkaEngineVersion.map { version -> create("org.jetbrains.dokka:$module:$version") }

        private fun AttributeContainer.dokkaPluginsClasspath() {
            attribute(DokkaFormatAttribute, formatDependencies.formatAttributes.format.name)
            attribute(DokkaClasspathAttribute, formatDependencies.baseAttributes.dokkaPlugins.name)
        }

        private fun AttributeContainer.dokkaGeneratorClasspath() {
            attribute(DokkaFormatAttribute, formatDependencies.formatAttributes.format.name)
            attribute(DokkaClasspathAttribute, formatDependencies.baseAttributes.dokkaGenerator.name)
        }

        /** Add a dependency to the Dokka plugins classpath */
        fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>): Unit =
            addProvider(
                dependencyContainerNames.pluginsClasspath,
                dependency,
                Action<ExternalModuleDependency> {
                    attributes { dokkaPluginsClasspath() }
                }
            )

        /** Add a dependency to the Dokka plugins classpath */
        fun DependencyHandler.dokkaPlugin(dependency: String) {
            add(dependencyContainerNames.pluginsClasspath, dependency) {
                attributes { dokkaPluginsClasspath() }
            }
        }

        /** Add a dependency to the Dokka Generator classpath */
        fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) {
            addProvider(
                dependencyContainerNames.generatorClasspath,
                dependency,
                Action<ExternalModuleDependency> {
                    attributes { dokkaGeneratorClasspath() }
                }
            )
        }

        /** Add a dependency to the Dokka Generator classpath */
        fun DependencyHandler.dokkaGenerator(dependency: String) {
            add(dependencyContainerNames.generatorClasspath, dependency) {
                attributes { dokkaGeneratorClasspath() }
            }
        }
    }

    private fun DokkaFormatPluginContext.addDefaultDokkaDependencies() {
        project.dependencies {
            dokkaPlugin(dokka("templating-plugin"))
            dokkaPlugin(dokka("dokka-base"))

            dokkaGenerator(
                if (project.pluginFeaturesService.enableK2Analysis) {
                    dokka("analysis-kotlin-symbols") // K2 analysis
                } else {
                    dokka("analysis-kotlin-descriptors") // K1 analysis
                }
            )
            dokkaGenerator(dokka("dokka-core"))
        }
    }

    companion object {
        private val logger = Logging.getLogger(DokkaFormatPlugin::class.java)
    }
}
