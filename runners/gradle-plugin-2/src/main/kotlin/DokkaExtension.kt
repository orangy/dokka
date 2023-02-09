package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaPublication
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaSourceSetGradleBuilder

/**
 * Configure the behaviour of the [DokkaPlugin].
 */
abstract class DokkaExtension {

    /** Default version used for Dokka dependencies */
    abstract val dokkaVersion: Property<String>

    /** Directory into which [DokkaPublication]s will be produced */
    abstract val dokkaPublicationDirectory: DirectoryProperty

    abstract val dokkaConfigurationsDirectory: DirectoryProperty

    /** Default Dokka cache directory */
    abstract val dokkaCacheDirectory: DirectoryProperty

//    abstract val dokkaConfigurations: NamedDomainObjectContainer<DokkaConfigurationGradleBuilder>

    abstract val moduleNameDefault: Property<String>
    abstract val moduleVersionDefault: Property<String>

    /**
     * String used to discriminate between source sets that originate from different Gradle subprojects
     *
     * Defaults to [the path of the subproject][org.gradle.api.Project.getPath].
     */
    abstract val sourceSetScopeDefault: Property<String>

    /**
     * Configuration for creating Dokka Publications.
     *
     * Each publication will generate one Dokka site based on the included Dokka Source Sets.
     *
     * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
     */
    abstract val dokkaPublications: NamedDomainObjectContainer<DokkaPublication>

    /**
     * Dokka Source Sets that describe source code in the local project (not subprojects)
     *
     * These source sets will be added to all [dokkaPublications].
     */
    abstract val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetGradleBuilder>
}
