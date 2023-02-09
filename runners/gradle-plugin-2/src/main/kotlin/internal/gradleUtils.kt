package org.jetbrains.dokka.gradle.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider


/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * ```
 */
internal fun Configuration.asProvider() {
    isCanBeResolved = false
    isCanBeConsumed = true
}

/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * ```
 * */
internal fun Configuration.asConsumer() {
    isCanBeResolved = true
    isCanBeConsumed = false
}


/** Invert a boolean [Provider] */
internal operator fun Provider<Boolean>.not(): Provider<Boolean> = map { !it }
