/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.TaskExecutionRequest
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.dokka.gradle.internal.PluginFeaturesService.PluginMode.*
import org.jetbrains.dokka.gradle.tasks.LogHtmlPublicationLinkTask
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * Internal utility service for managing Dokka Plugin features and warnings.
 *
 * Using a [BuildService] is most useful for only logging a single warning for the whole project,
 * regardless of how many subprojects have applied DGP.
 */
internal abstract class PluginFeaturesService
@InternalDokkaGradlePluginApi
@Inject
constructor(
    providers: ProviderFactory,
) : BuildService<PluginFeaturesService.Params> {

    interface Params : BuildServiceParameters {
        /** @see [PluginFeaturesService.primaryService] */
        val primaryService: Property<Boolean>

        /** @see PluginFeaturesService.pluginMode */
        val pluginMode: Property<String>

        /** If `true`, suppress [pluginMode] messages. */
        val pluginModeNoWarn: Property<Boolean>

        /** If `true`, enable K2 analysis. */
        val k2AnalysisEnabled: Property<Boolean>

        /** If `true`, suppress [k2AnalysisEnabled] messages. */
        val k2AnalysisNoWarn: Property<Boolean>

        /** @see PluginFeaturesService.dumpDokkaConfigurationDebugFile */
        val dumpDokkaConfigurationDebugFile: Property<Boolean>

        /** [Project.getDisplayName] - only used for log messages. */
        val projectDisplayName: Property<String>

        /** [Project.getProjectDir] - only used for log messages. */
        val projectDirectory: Property<String>
    }

    /**
     * Designate this [BuildService] as 'primary', meaning it should log messages to users.
     * Non-primary services should not log messages.
     *
     * Why? Two reasons:
     *
     * 1. Sometimes registering a BuildService fails.
     *   See https://github.com/gradle/gradle/issues/17559.
     *   If service registration fails then re-register the service, but with a distinct name
     *   (so it doesn't clash with the existing but inaccessible BuildService),
     *   and don't mark it as 'primary' to avoid duplicate logging.
     * 2. The method Gradle uses to generates accessors for pre-compiled script plugins
     *   (see [PluginFeaturesService.Companion.configureParamsDuringAccessorsGeneration])
     *   runs in a temporary project that is evaluated twice.
     *
     * @see org.jetbrains.dokka.gradle.internal.registerIfAbsent
     */
    private val primaryService: Boolean
        get() = parameters.primaryService.getOrElse(false)

    /**
     * Whether DGP should use V2 [org.jetbrains.dokka.gradle.DokkaBasePlugin].
     *
     * @see pluginMode
     */
    internal val v2PluginEnabled: Boolean by lazy {
        when (pluginMode) {
            V1Enabled,
                -> false

            V2EnabledWithHelpers,
            V2Enabled,
                -> true
        }
    }

    /**
     * Enable some migration helpers to aid in migrating DGP from V1 to V2.
     *
     * @see addV2MigrationHelpers
     */
    internal val v2PluginMigrationHelpersEnabled: Boolean by lazy {
        pluginMode == V2EnabledWithHelpers
    }

    /**
     * Determines the behaviour of DGP.
     *
     * If no valid value is detected then use [PluginMode.Default].
     */
    private val pluginMode: PluginMode by lazy {
        val parameterValue = parameters.pluginMode.getOrElse(PluginMode.Default.name)

        val value = PluginMode.findOrNull(parameterValue)
            ?: run {
                logger.warn("Invalid value for $PLUGIN_MODE_FLAG. Got '$parameterValue' but expected one of: ${PluginMode.values.map { it.name }}")
                PluginMode.Default
            }

        logger.info { "Dokka Gradle Plugin detected PluginMode:$value (from:$parameterValue) in ${parameters.projectDisplayName.orNull} ${parameters.projectDirectory.orNull}" }

        logPluginMessage(value)

        value
    }

    private val pluginModeNoWarn: Boolean by lazy {
        parameters.pluginModeNoWarn.getOrElse(false)
    }

    private fun logPluginMessage(mode: PluginMode) {
        when (mode) {
            V1Enabled -> logV1PluginMessage()
            V2EnabledWithHelpers -> logV2PluginMigrationMessage()
            V2Enabled -> logV2PluginMessage()
        }
    }

    private fun logV1PluginMessage() {
        if (primaryService && !pluginModeNoWarn) {
            logger.warn("warning: Dokka Gradle plugin V1 is deprecated")
            logger.lifecycle(
                """
                |
                |Dokka Gradle plugin V1 is deprecated, and will be removed in Dokka version 2.1.0
                |Please migrate to Dokka Gradle plugin V2. This will require updating your project.
                |$`To learn about migrating read the migration guide`
                |
                |To start migrating to Dokka Gradle plugin V2 add
                |    ${PLUGIN_MODE_FLAG}=${V2EnabledWithHelpers}
                |into your project's `gradle.properties` file.
                |
                |$`We would appreciate your feedback`
                |
                """.trimMargin().prependIndent()
            )
        }
    }

    private fun logV2PluginMigrationMessage() {
        if (primaryService) {
            // Migration helpers are provided as a temporary solution to aid with migration to V2,
            // and they should not be enabled long term because they are non-functional.
            // To encourage disabling them always log a warning, regardless of the noWarn flag.
            logger.warn("warning: Dokka Gradle plugin V2 migration helpers are enabled")
        }
        if (primaryService && !pluginModeNoWarn) {
            logger.lifecycle(
                """
                |
                |Thank you for migrating to Dokka Gradle plugin V2!
                |Migration is in progress, and helpers have been enabled.
                |$`To learn about migrating read the migration guide`
                |
                |Once you have finished migrating disable the migration helpers by adding
                |    ${PLUGIN_MODE_FLAG}=${V2Enabled}
                |to your project's `gradle.properties` file.
                |
                |$`We would appreciate your feedback`
                |
                """.trimMargin().prependIndent()
            )
        }
    }

    private fun logV2PluginMessage() {
        if (primaryService && !pluginModeNoWarn) {
            logger.lifecycle(
                """
                |
                |Thank you for enabling Dokka Gradle plugin V2!
                |$`To learn about migrating read the migration guide`
                |
                |$`We would appreciate your feedback`
                |
                |You can suppress this message by adding
                |    ${PLUGIN_MODE_NO_WARN_FLAG_PRETTY}=true
                |to your project's `gradle.properties` file.
                |
                """.trimMargin().prependIndent()
            )
        }
    }

    internal val enableK2Analysis: Boolean by lazy {
        // use lazy {} to ensure messages are only logged once

        val enableK2Analysis = parameters.k2AnalysisEnabled.getOrElse(false)

        if (enableK2Analysis) {
            logK2AnalysisMessage()
        }

        enableK2Analysis
    }

    private fun logK2AnalysisMessage() {
        if (primaryService && !parameters.k2AnalysisNoWarn.getOrElse(false)) {
            logger.warn("warning: Dokka K2 Analysis is enabled")
            logger.lifecycle(
                """
                |Dokka K2 Analysis is Experimental and is still under active development.
                |It can cause build failures or generate incorrect documentation. 
                |
                |$`We would appreciate your feedback`
                |
                |You can suppress this message by adding
                |    ${K2_ANALYSIS_NO_WARN_FLAG}=true
                |to your project's `gradle.properties` file.
                """.trimMargin().prependIndent()
            )
        }
    }

    /**
     * Enable saving the [org.jetbrains.dokka.DokkaConfiguration] used to run
     * [org.jetbrains.dokka.DokkaGenerator] to a file.
     *
     * The configuration file is only useful for debugging.
     * (For example, checking the generated configuration on CI, which might use Linux/Windows/macOS).
     *
     * @see org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask.dokkaConfigurationJsonFile
     */
    internal val dumpDokkaConfigurationDebugFile: Boolean by lazy {
        parameters.dumpDokkaConfigurationDebugFile.getOrElse(false)
    }

    /** Values for [pluginMode]. */
    private enum class PluginMode {
        V1Enabled,
        V2EnabledWithHelpers,
        V2Enabled,
        ;

        companion object {
            /** The default value, if [Params.pluginMode] is not set. */
            val Default: PluginMode = V1Enabled

            val values: Set<PluginMode> = values().toSet()

            fun findOrNull(value: String): PluginMode? =
                values.find { it.name == value }
        }
    }

    /**
     * Control whether the [LogHtmlPublicationLinkTask] task is enabled.
     *
     * Useful for disabling the task locally, or in CI/CD, or for tests.
     *
     * It can be set in any `gradle.properties` file. For example, on a single user's machine:
     *
     * ```properties
     * # $GRADLE_USER_HOME/gradle.properties
     * org.jetbrains.dokka.gradle.enabledLogHtmlPublicationLink=false
     * ```
     *
     * or via an environment variable
     *
     * ```env
     * ORG_GRADLE_PROJECT_org.jetbrains.dokka.gradle.enabledLogHtmlPublicationLink=false
     * ```
     */
    val enableLogHtmlPublicationLink: Provider<Boolean> =
        providers.gradleProperty("org.jetbrains.dokka.gradle.enableLogHtmlPublicationLink")
            .toBoolean()
            .orElse(true)

    companion object {
        private val logger = Logging.getLogger(PluginFeaturesService::class.java)

        /** @see [PluginFeaturesService.pluginMode] */
        private const val PLUGIN_MODE_FLAG =
            "org.jetbrains.dokka.experimental.gradle.pluginMode"

        /** @see [PluginFeaturesService.pluginModeNoWarn] */
        private const val PLUGIN_MODE_NO_WARN_FLAG =
            "$PLUGIN_MODE_FLAG.nowarn"

        /** The same as [PLUGIN_MODE_NO_WARN_FLAG], but it doesn't trigger spell-checks. */
        private const val PLUGIN_MODE_NO_WARN_FLAG_PRETTY =
            "$PLUGIN_MODE_FLAG.noWarn"

        private const val K2_ANALYSIS_ENABLED_FLAG =
            "org.jetbrains.dokka.experimental.tryK2"

        private const val K2_ANALYSIS_NO_WARN_FLAG =
            "$K2_ANALYSIS_ENABLED_FLAG.nowarn"

        private const val K2_ANALYSIS_NO_WARN_FLAG_PRETTY =
            "$K2_ANALYSIS_ENABLED_FLAG.noWarn"

        /** @see PluginFeaturesService.dumpDokkaConfigurationDebugFile */
        private const val DUMP_DOKKA_CONFIG_DEBUG_FILE =
            "org.jetbrains.dokka.internal.dumpDokkaConfigurationDebugFile"

        @Suppress("ObjectPrivatePropertyName")
        private val `To learn about migrating read the migration guide` = /* language=text */ """
            |To learn about migrating read the migration guide https://kotl.in/dokka-gradle-migration
        """.trimMargin()

        @Suppress("ObjectPrivatePropertyName")
        private val `We would appreciate your feedback` = /* language=text */ """
            |We would appreciate your feedback!
            | - Please report any feedback or problems https://kotl.in/dokka-issues
            | - Chat with the community visit #dokka in https://kotlinlang.slack.com/ (To sign up visit https://kotl.in/slack)
        """.trimMargin()

        /**
         * Register a new [PluginFeaturesService], or get an existing instance.
         */
        val Project.pluginFeaturesService: PluginFeaturesService
            get() = getOrCreateService(project)

        private fun getOrCreateService(project: Project): PluginFeaturesService {
            val configureServiceParams = serviceParamsConfiguration(project)

            return try {
                project.gradle.sharedServices.registerIfAbsent(PluginFeaturesService::class) {
                    parameters(configureServiceParams)
                    // This service was successfully registered, so it is considered 'primary'.
                    parameters.primaryService.set(true)
                }.get()
            } catch (ex: ClassCastException) {
                try {
                    // Recover from Gradle bug: re-register the service, but don't mark it as 'primary'.
                    project.gradle.sharedServices.registerIfAbsent(
                        PluginFeaturesService::class,
                        classLoaderScoped = true,
                    ) {
                        parameters(configureServiceParams)
                        parameters.primaryService.set(false)
                    }.get()
                } catch (ex: ClassCastException) {
                    throw GradleException(
                        "Failed to register BuildService. Please report this problem https://github.com/gradle/gradle/issues/17559",
                        ex
                    )
                }
            }
        }

        /**
         * Return an [Action] that will configure [PluginFeaturesService.Params], based on detected plugin flags.
         */
        private fun serviceParamsConfiguration(
            project: Project
        ): Action<Params> {

            /** Find a flag for [PluginFeaturesService]. */
            fun getFlag(flag: String): Provider<String> =
                project.providers
                    .gradleProperty(flag)
                    .forUseAtConfigurationTimeCompat()
                    .orElse(
                        // Note: Enabling/disabling features via extra-properties is only intended for unit tests.
                        // (Because org.gradle.testfixtures.ProjectBuilder doesn't support mocking Gradle properties.
                        // But maybe soon! https://github.com/gradle/gradle/pull/30002)
                        project
                            .provider { project.extra.properties[flag]?.toString() }
                            .forUseAtConfigurationTimeCompat()
                    )

            return Action {
                projectDirectory.set(project.projectDir.invariantSeparatorsPath)
                projectDisplayName.set(project.displayName)

                pluginMode.set(getFlag(PLUGIN_MODE_FLAG))
                pluginModeNoWarn.set(
                    getFlag(PLUGIN_MODE_NO_WARN_FLAG)
                        .orElse(getFlag(PLUGIN_MODE_NO_WARN_FLAG_PRETTY))
                        .toBoolean()
                )

                k2AnalysisEnabled.set(getFlag(K2_ANALYSIS_ENABLED_FLAG).toBoolean())
                k2AnalysisNoWarn.set(
                    getFlag(K2_ANALYSIS_NO_WARN_FLAG_PRETTY)
                        .orElse(getFlag(K2_ANALYSIS_NO_WARN_FLAG))
                        .toBoolean()
                )

                dumpDokkaConfigurationDebugFile.set(getFlag(DUMP_DOKKA_CONFIG_DEBUG_FILE).toBoolean())

                configureParamsDuringAccessorsGeneration(project)
            }
        }

        /**
         * We use a Gradle flag to control whether DGP is in v1 or v2 mode.
         * This flag dynamically changes the behaviour of DGP at runtime.
         *
         * However, there is a particular situation where this flag can't be detected:
         * When Dokka is applied to a precompiled script plugin and Gradle generates Kotlin DSL accessors.
         *
         * When Gradle is generating such accessors, it creates a temporary project, totally disconnected
         * from the main build. The temporary project has no access to any Gradle properties.
         * As such, no Dokka flags can be detected, resulting in unexpected behaviour.
         *
         * We work around this by first detecting when Gradle is generating accessors
         * (see [isGradleGeneratingAccessors]), and secondly by manually discovering a suitable
         * `gradle.properties` file (see [findGradlePropertiesFile]) and reading its values.
         *
         * This is a workaround and can be removed with DGPv1
         * https://youtrack.jetbrains.com/issue/KT-71027/
         */
        private fun Params.configureParamsDuringAccessorsGeneration(project: Project) {
            try {
                if (project.isGradleGeneratingAccessors()) {
                    logger.info("Gradle is generating accessors. Discovering Dokka Gradle Plugin flags manually. ${project.gradle.rootProject.name} | ${project.gradle.rootProject.rootDir}")

                    // Disable all warnings, regardless of the discovered flag values.
                    // Log messages will be printed too soon and aren't useful for users.
                    primaryService.set(false)
                    pluginModeNoWarn.set(true)

                    // Because Gradle is generating accessors, it won't give us access to Gradle properties
                    // defined for the main project. So, we must discover `gradle.properties` ourselves.
                    val propertiesFile = project.findGradlePropertiesFile()

                    val properties = Properties().apply {
                        propertiesFile?.reader()?.use { reader ->
                            load(reader)
                        }
                    }

                    // These are the only flags that are important when Gradle is generating accessors,
                    // because they control what accessors DGP registers.
                    properties[PLUGIN_MODE_FLAG]?.toString()?.let {
                        pluginMode.set(it)
                    }
                }
            } catch (t: Throwable) {
                // Ignore all errors.
                // This is just a temporary util. It doesn't need to be stable long-term,
                // and we don't want to risk breaking people's projects.
            }
        }
    }
}


/**
 * Determine if Gradle is generating DSL script accessors for precompiled script plugins.
 *
 * When Gradle generates accessors, it creates an empty project in a temporary directory and runs no tasks.
 * So, we can guess whether Gradle is generating accessors based on this information.
 */
private fun Project.isGradleGeneratingAccessors(): Boolean {
    if (gradle.rootProject.name != "gradle-kotlin-dsl-accessors") {
        return false
    }

    if (gradle.taskGraph.allTasks.isNotEmpty()) {
        return false
    }

    /**
     * When a Gradle build is executed with no requested tasks and no arguments then
     * Gradle runs a single 'default' task that has no values.
     */
    fun TaskExecutionRequest.isDefaultTask(): Boolean =
        projectPath == null && args.isEmpty() && rootDir == null

    val taskRequest = gradle.startParameter.taskRequests.singleOrNull() ?: return false
    if (!taskRequest.isDefaultTask()) return false

    // Gradle generates accessors in a temporary project in a temporary directory, e.g.
    //    `build-logic/build/tmp/generatePrecompiledScriptPluginAccessors/accessors373648437747350006`
    // The last directory has a random name, so we can drop that and check if the other segments match.
    val rootProjectPath = gradle.rootProject.rootDir.invariantSeparatorsPath
    return rootProjectPath
        .substringBeforeLast("/")
        .endsWith("build/tmp/generatePrecompiledScriptPluginAccessors")
}


/**
 * Walk up the file tree until we discover a `gradle.properties` file.
 *
 * Note that this function will harm Configuration Cache, because it accesses files during the configuration phase.
 * It must only be used
 */
private fun Project.findGradlePropertiesFile(): File? {
    return generateSequence(project.projectDir) { it.parentFile }
        .takeWhile { it != it.parentFile && it.exists() }

        // Add an arbitrary limit to stop infinite scanning, just in case something goes wrong
        .take(50)

        // Skip the first 5 directories, to get to the actual project directory.
        // <actual project dir>/build/tmp/generatePrecompiledScriptPluginAccessors/accessors373648437747350006
        //   ^5                  ^4    ^3                  ^2                                ^1
        .drop(5)

        .map { it.resolve("gradle.properties") }

        .firstOrNull { it.exists() && it.isFile }
}
