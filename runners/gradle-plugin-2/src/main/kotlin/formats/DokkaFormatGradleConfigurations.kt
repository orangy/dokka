package org.jetbrains.dokka.gradle.formats

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration

/**
 * The Dokka-specific Gradle [Configuration]s used to produce and consume files from external sources
 * (example: Maven Central), or between subprojects.
 */
internal data class DokkaFormatGradleConfigurations(

//    /** Fetch all Dokka files from all configurations in other subprojects */
//    val dokkaConsumer: NamedDomainObjectProvider<Configuration>,


    /** Fetch Dokka Configuration files from other subprojects */
    val dokkaConfigurationsConsumer: NamedDomainObjectProvider<Configuration>,
    /** Provide Dokka Configurations files to other subprojects */
    val dokkaConfigurationsElements: NamedDomainObjectProvider<Configuration>,


    /**
     * Runtime classpath used to execute Dokka Worker.
     *
     * Extends [dokkaPluginsClasspath].
     *
     * @see org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker
     * @see org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
     */
    val dokkaGeneratorClasspath: NamedDomainObjectProvider<Configuration>,

    /**
     * Dokka plugins.
     *
     * Users can add plugins to this dependency.
     *
     * Should not contain runtime dependencies.
     */
    val dokkaPluginsClasspath: NamedDomainObjectProvider<Configuration>,

    /**
     * Dokka plugins, without transitive dependencies.
     *
     * It extends [dokkaPluginsClasspath], so do not add dependencies to this configuration -
     * the dependencies are computed automatically.
     */
    val dokkaPluginsIntransitiveClasspath: NamedDomainObjectProvider<Configuration>,
)
