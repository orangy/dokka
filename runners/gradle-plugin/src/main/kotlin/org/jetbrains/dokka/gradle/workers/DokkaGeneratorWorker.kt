package org.jetbrains.dokka.gradle.workers

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.gradle.internal.LoggerAdapter
import org.jetbrains.dokka.gradle.internal.toJsonString

/**
 * Gradle Worker Daemon for running [DokkaGenerator].
 *
 * The worker requires [DokkaGenerator] is present on the classpath.
 */
abstract class DokkaGeneratorWorker : WorkAction<DokkaGeneratorWorker.Parameters> {

    private val logger = LoggerAdapter(DokkaGeneratorWorker::class)

    interface Parameters : WorkParameters {
        val dokkaConfiguration: Property<DokkaConfigurationImpl>
    }

    override fun execute() {
        val dokkaConfiguration = parameters.dokkaConfiguration.get()

        logger.progress("Dokka configuration:\n${toJsonString(dokkaConfiguration)}")

        val generator = DokkaGenerator(dokkaConfiguration, logger)

        generator.generate()
    }
}
