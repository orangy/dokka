/*
* Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
*/
package org.jetbrains.dokka.gradle.utils

import org.gradle.api.logging.LogLevel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The arguments in this class will be used to build the project's
 * `gradle.properties` file.
 */
data class GradlePropertiesBuilder(
    val gradle: GradleArgs = GradleArgs(),
    val dokka: DokkaArgs = DokkaArgs(),
    val kotlin: KotlinArgs = KotlinArgs(),
) {
    fun dokka(config: DokkaArgs.() -> Unit): Unit = dokka.config()
    fun gradle(config: GradleArgs.() -> Unit): Unit = gradle.config()
    fun kotlin(config: KotlinArgs.() -> Unit): Unit = kotlin.config()

    /** Gradle specific options. */
    data class GradleArgs(
        var logLevel: LogLevel? = LogLevel.LIFECYCLE,
        /** Valid values are: `internal`, `all`, `full`. */
        var stacktrace: String? = "all",
        var debug: Boolean? = null,
        var buildCache: Boolean? = true,
        var buildCacheDebugLog: Boolean? = null,
        var configurationCache: Boolean? = null,
        var configureOnDemand: Boolean? = null,
        var continueOnFailure: Boolean? = null,
        var parallel: Boolean? = null,
        var warningMode: org.gradle.api.logging.configuration.WarningMode? = null,
        /** Will be enabled by default in Gradle 9.0 */
        var kotlinDslSkipMetadataVersionCheck: Boolean? = true,
        var daemonIdleTimeout: Duration? = 30.seconds,
        /**
         * Specifies the scheduling priority for the Gradle daemon and all processes launched by it.
         */
        var daemonSchedulingPriority: SchedulingPriority? = SchedulingPriority.Low,
        var maxWorkers: Int? = null,
        val jvmArgs: JvmArgs = JvmArgs(),

        // Maybe also implement these flags? Although there's no suitable tests for them at present.
        // org.gradle.projectcachedir=(directory)
        // org.gradle.unsafe.isolated-projects=(true,false)
        // org.gradle.vfs.verbose=(true,false)
        // org.gradle.vfs.watch=(true,false)
    ) {
        enum class SchedulingPriority { Low, Normal }

        fun jvm(config: JvmArgs.() -> Unit): Unit = jvmArgs.config()
    }

    /** Kotlin specific options. */
    data class KotlinArgs(
        var mppStabilityWarning: Boolean? = true,
    )

    /** Dokka specific options. */
    data class DokkaArgs(
        /** @see org.jetbrains.dokka.gradle.internal.PluginFeaturesService.PluginMode */
        var pluginMode: String? = "V2Enabled",
        var pluginModeNoWarn: Boolean? = true,
        var k2Analysis: Boolean? = null,
        var k2AnalysisNoWarn: Boolean? = null,
        var enableLogHtmlPublicationLink: Boolean? = false,
    )

    /** Gradle Daemon JVM args. */
    data class JvmArgs(
        @Suppress("PropertyName")
        var Xmx: String? = null,
        var fileEncoding: String? = "UTF-8",
        var maxMetaspaceSize: String? = "512m",
        /** Enable `AlwaysPreTouch` by default https://github.com/gradle/gradle/issues/3093#issuecomment-387259298 */
        var alwaysPreTouch: Boolean = true,
    ) {
        fun buildString(): String = buildList {
            fun addNotNull(key: String, value: String?) {
                value?.let { add("$key$it") }
            }

            addNotNull("-Xmx", Xmx)
            addNotNull("-XX:MaxMetaspaceSize=", maxMetaspaceSize)
            addNotNull("-Dfile.encoding=", fileEncoding)
            if (alwaysPreTouch) add("-XX:+AlwaysPreTouch")
        }.joinToString(" ")
    }

    fun build(): Map<String, String> = buildMap {
        fun putNotNull(key: String, value: Any?) {
            value?.let { put(key, value.toString()) }
        }

        with(dokka) {
            putNotNull("org.jetbrains.dokka.experimental.gradle.pluginMode", pluginMode)
            putNotNull("org.jetbrains.dokka.experimental.gradle.pluginMode.noWarn", pluginModeNoWarn)
            putNotNull("org.jetbrains.dokka.experimental.tryK2", k2Analysis)
            putNotNull("org.jetbrains.dokka.experimental.tryK2.noWarn", k2AnalysisNoWarn)
            putNotNull("org.jetbrains.dokka.gradle.enableLogHtmlPublicationLink", enableLogHtmlPublicationLink)
        }

        with(kotlin) {
            putNotNull("kotlin.mpp.stability.nowarn", mppStabilityWarning?.let { !it })
        }

        with(gradle) {
            putNotNull("org.gradle.caching", buildCache)
            putNotNull("org.gradle.caching.debug", buildCacheDebugLog)
            putNotNull("org.gradle.configuration-cache", configurationCache)
            putNotNull("org.gradle.configureondemand", configureOnDemand)
            putNotNull("org.gradle.continue", continueOnFailure)
            putNotNull("org.gradle.daemon.idletimeout", daemonIdleTimeout?.inWholeMilliseconds)
            putNotNull("org.gradle.priority", daemonSchedulingPriority?.name?.lowercase())
            putNotNull("org.gradle.debug", debug)
            putNotNull("org.gradle.logging.level", logLevel?.name?.lowercase())
            putNotNull("org.gradle.workers.max", maxWorkers)
            putNotNull("org.gradle.parallel", parallel)
            putNotNull("org.gradle.stacktrace", stacktrace)
            putNotNull("org.gradle.warning.mode", warningMode)
            jvmArgs.buildString().takeIf { it.isNotBlank() }?.let {
                put("org.gradle.jvmargs", it)
            }
        }
    }
}
