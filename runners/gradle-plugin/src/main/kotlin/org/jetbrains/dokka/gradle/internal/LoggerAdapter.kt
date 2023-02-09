package org.jetbrains.dokka.gradle.internal

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.dokka.utilities.DokkaLogger
import kotlin.reflect.KClass

/**
 * Adapt a Gradle [Logger] to a [DokkaLogger], to be used by Dokka when generating documentation.
 *
 * Using the Gradle logger means that the log-level wil be controlled by the standard
 * Gradle command line options, e.g. `--info`.
 *
 * @param[logger] A Gradle logger
 * @see org.jetbrains.dokka.DokkaGenerator
 */
internal class LoggerAdapter(
    private val logger: Logger
) : DokkaLogger {

    constructor(kClass: KClass<*>) : this(Logging.getLogger(kClass.java))

    override var errorsCount: Int = 0
    override var warningsCount: Int = 0

    override fun debug(message: String) = logger.debug(message)
    override fun progress(message: String) = logger.lifecycle(message)
    override fun info(message: String) = logger.info(message)

    override fun warn(message: String) {
        warningsCount++
        logger.warn(message)
    }

    override fun error(message: String) {
        errorsCount++
        logger.error(message)
    }
}
