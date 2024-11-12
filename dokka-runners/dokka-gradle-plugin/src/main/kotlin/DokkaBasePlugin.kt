/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.gradle.dependencies.BaseDependencyManager
import org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.Companion.DokkaClasspathAttribute
import org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.Companion.DokkaFormatAttribute
import org.jetbrains.dokka.gradle.dependencies.DokkaAttribute.Companion.DokkaModuleComponentAttribute
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.dokka.gradle.internal.*
import org.jetbrains.dokka.gradle.tasks.DokkaBaseTask
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateModuleTask
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.dokka.gradle.tasks.TaskNames
import java.io.File
import javax.inject.Inject

/**
 * The base plugin for Dokka. Sets up Dokka tasks, configurations, etc., and configures default values,
 * but does not add any specific config (specifically, it does not create Dokka Publications).
 */
abstract class DokkaBasePlugin
@InternalDokkaGradlePluginApi
@Inject
constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
    private val objects: ObjectFactory,
) : Plugin<Project> {

    override fun apply(target: Project) {
        if (CurrentGradleVersion < minimumSupportedGradleVersion) {
            val currentGradleMajorVersion = CurrentGradleVersion.version.substringBefore(".")
            val updateGradleLink =
                "https://docs.gradle.org/current/userguide/upgrading_version_${currentGradleMajorVersion}.html"
            logger.error(
                """
                |Failed to apply ${DokkaBasePlugin::class.simpleName} in project ${target.displayName}. 
                |The minimum supported Gradle version is $minimumSupportedGradleVersion, but the current Gradle version is $CurrentGradleVersion.
                |Please update your Gradle version $updateGradleLink
                """.trimMargin()
            )
            return
        }

        // apply the lifecycle-base plugin so the clean task is available
        target.pluginManager.apply(LifecycleBasePlugin::class)

        val dokkaExtension = createExtension(target)

        configureDependencyAttributes(target)

        configureDokkaPublicationsDefaults(dokkaExtension)

        initDokkaTasks(target, dokkaExtension)
    }

    private fun createExtension(project: Project): DokkaExtension {

        val baseDependencyManager = BaseDependencyManager(
            project = project,
            objects = objects,
        )

        val dokkaExtension = project.extensions.create<DokkaExtension>(
            EXTENSION_NAME,
            baseDependencyManager,
        ).apply {
            moduleName.convention(providers.provider { project.name })
            moduleVersion.convention(providers.provider { project.version.toString() })
            modulePath.convention(project.pathAsFilePath())
            konanHome.convention(
                providers
                    .provider {
                        // konanHome is set into in extraProperties:
                        // https://github.com/JetBrains/kotlin/blob/v1.9.0/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/KotlinNativeTargetPreset.kt#L35-L38
                        project.extensions.extraProperties.get("konanHome") as? String?
                    }
                    .map { File(it) }
            )

            sourceSetScopeDefault.convention(project.path)
            basePublicationsDirectory.convention(layout.buildDirectory.dir("dokka"))
            baseModulesDirectory.convention(layout.buildDirectory.dir("dokka-module"))
            dokkaEngineVersion.convention(DokkaConstants.DOKKA_VERSION)
        }

        dokkaExtension.dokkaGeneratorIsolation.convention(
            dokkaExtension.ProcessIsolation {
                maxHeapSize.convention("2g")
                debug.convention(false)
                jvmArgs.convention(
                    listOf(
                        //"-XX:MaxMetaspaceSize=512m",
                        "-XX:+HeapDumpOnOutOfMemoryError",
                        "-XX:+AlwaysPreTouch", // https://github.com/gradle/gradle/issues/3093#issuecomment-387259298
                        //"-XX:StartFlightRecording=disk=true,name={path.drop(1).map { if (it.isLetterOrDigit()) it else '-' }.joinToString("")},dumponexit=true,duration=30s",
                        //"-XX:FlightRecorderOptions=repository=$baseDir/jfr,stackdepth=512",
                    )
                )
            }
        )

        @Suppress("DEPRECATION")
        dokkaExtension.suppressInheritedMembers.convention(false)
        @Suppress("DEPRECATION")
        dokkaExtension.suppressObviousFunctions.convention(true)

        dokkaExtension.dokkaSourceSets.configureDefaults(
            sourceSetScopeConvention = dokkaExtension.sourceSetScopeDefault
        )

        return dokkaExtension
    }


    private fun configureDependencyAttributes(target: Project) {
        target.dependencies.attributesSchema {
            attribute(DokkaFormatAttribute)
            attribute(DokkaModuleComponentAttribute)
            attribute(DokkaClasspathAttribute)
        }
    }


    /** Set defaults in all [DokkaExtension.dokkaPublications]s */
    private fun configureDokkaPublicationsDefaults(
        dokkaExtension: DokkaExtension,
    ) {
        dokkaExtension.dokkaPublications.all {
            enabled.convention(true)
            cacheRoot.convention(dokkaExtension.dokkaCacheDirectory)
            failOnWarning.convention(false)
            finalizeCoroutines.convention(false)
            moduleName.convention(dokkaExtension.moduleName)
            moduleVersion.convention(dokkaExtension.moduleVersion)
            offlineMode.convention(false)
            outputDirectory.convention(dokkaExtension.basePublicationsDirectory.dir(formatName))
            moduleOutputDirectory.convention(dokkaExtension.baseModulesDirectory.dir(formatName))
            suppressInheritedMembers.convention(
                @Suppress("DEPRECATION")
                dokkaExtension.suppressInheritedMembers
            )
            suppressObviousFunctions.convention(
                @Suppress("DEPRECATION")
                dokkaExtension.suppressObviousFunctions
            )
        }
    }


    /** Set conventions for all [DokkaSourceSetSpec] properties */
    private fun NamedDomainObjectContainer<DokkaSourceSetSpec>.configureDefaults(
        sourceSetScopeConvention: Property<String>,
    ) {
        configureEach dss@{
            analysisPlatform.convention(KotlinPlatform.DEFAULT)
            displayName.convention(
                analysisPlatform.map { platform ->
                    // Match existing Dokka naming conventions. (This should probably be simplified!)
                    when {
                        // Multiplatform source sets (e.g. commonMain, jvmMain, macosMain)
                        name.endsWith("Main") -> name.substringBeforeLast("Main")

                        // indeterminate source sets should use the name of the Kotlin platform
                        else -> platform.displayName
                    }
                }
            )
            documentedVisibilities.convention(setOf(VisibilityModifier.Public))
            jdkVersion.convention(11)

            enableKotlinStdLibDocumentationLink.convention(true)
            enableJdkDocumentationLink.convention(true)
            enableAndroidDocumentationLink.convention(
                analysisPlatform.map { it == KotlinPlatform.AndroidJVM }
            )

            reportUndocumented.convention(false)
            skipDeprecated.convention(false)
            skipEmptyPackages.convention(true)
            sourceSetScope.convention(sourceSetScopeConvention)

            // Manually added sourceSets should not be suppressed by default.
            // dokkaSourceSets that are automatically added by DokkaKotlinAdapter will have a sensible value for 'suppress'.
            suppress.convention(false)

            suppressGeneratedFiles.convention(true)

            sourceLinks.configureEach {
                localDirectory.convention(layout.projectDirectory)
                remoteLineSuffix.convention("#L")
            }

            perPackageOptions.configureEach {
                matchingRegex.convention(".*")
                suppress.convention(false)
                skipDeprecated.convention(false)
                reportUndocumented.convention(false)
                documentedVisibilities.convention(listOf(VisibilityModifier.Public))
            }

            externalDocumentationLinks {
                configureEach {
                    enabled.convention(true)
                    packageListUrl.convention(url.map { it.appendPath("package-list") })
                }

                maybeCreate("jdk") {
                    enabled.set(this@dss.enableJdkDocumentationLink)
                    url(this@dss.jdkVersion.map { jdkVersion ->
                        when {
                            jdkVersion < 11 -> "https://docs.oracle.com/javase/${jdkVersion}/docs/api/"
                            else -> "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/"
                        }
                    })
                    packageListUrl(this@dss.jdkVersion.map { jdkVersion ->
                        when {
                            jdkVersion < 11 -> "https://docs.oracle.com/javase/${jdkVersion}/docs/api/package-list"
                            else -> "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/element-list"
                        }
                    })
                }

                maybeCreate("kotlinStdlib") {
                    enabled.set(this@dss.enableKotlinStdLibDocumentationLink)
                    url("https://kotlinlang.org/api/latest/jvm/stdlib/")
                }

                maybeCreate("androidSdk") {
                    enabled.set(this@dss.enableAndroidDocumentationLink)
                    url("https://developer.android.com/reference/kotlin/")
                }

                maybeCreate("androidX") {
                    enabled.set(this@dss.enableAndroidDocumentationLink)
                    url("https://developer.android.com/reference/kotlin/")
                    packageListUrl("https://developer.android.com/reference/kotlin/androidx/package-list")
                }
            }
        }
    }


    private fun initDokkaTasks(
        target: Project,
        dokkaExtension: DokkaExtension,
    ) {
        target.tasks.register<DokkaBaseTask>(taskNames.generate) {
            description = "Generates Dokka publications for all formats"
            dependsOn(target.tasks.withType<DokkaGenerateTask>())
        }

        target.tasks.withType<DokkaGenerateTask>().configureEach {
            cacheDirectory.convention(dokkaExtension.dokkaCacheDirectory)
            workerLogFile.convention(temporaryDir.resolve("dokka-worker.log"))
            dokkaConfigurationJsonFile.convention(temporaryDir.resolve("dokka-configuration.json"))
            workerIsolation.convention(dokkaExtension.dokkaGeneratorIsolation)
            publicationEnabled.convention(true)
            onlyIf("publication must be enabled") { publicationEnabled.getOrElse(true) }

            generator.dokkaSourceSets.addAllLater(
                providers.provider {
                    // exclude suppressed source sets as early as possible, to avoid unnecessary dependency resolution
                    dokkaExtension.dokkaSourceSets.filterNot { it.suppress.get() }
                }
            )

            generator.dokkaSourceSets.configureDefaults(
                sourceSetScopeConvention = dokkaExtension.sourceSetScopeDefault
            )
        }

        target.tasks.withType<DokkaGenerateModuleTask>().configureEach {
            modulePath.convention(dokkaExtension.modulePath)
        }
    }


    //region workaround for https://github.com/gradle/gradle/issues/23708
    private fun RegularFileProperty.convention(file: File): RegularFileProperty =
        convention(objects.fileProperty().fileValue(file))

    private fun RegularFileProperty.convention(file: Provider<File>): RegularFileProperty =
        convention(objects.fileProperty().fileProvider(file))
    //endregion


    companion object {
        const val EXTENSION_NAME = "dokka"

        /**
         * The group of all Dokka [Gradle tasks][org.gradle.api.Task].
         *
         * @see org.gradle.api.Task.getGroup
         */
        const val TASK_GROUP = "dokka"

        /** The names of [Gradle tasks][org.gradle.api.Task] created by Dokka */
        val taskNames = TaskNames("")

        /** Name of the [Configuration] used to declare dependencies on other subprojects. */
        const val DOKKA_CONFIGURATION_NAME = "dokka"

        /** Name of the [Configuration] used to declare dependencies on Dokka Generator plugins. */
        const val DOKKA_GENERATOR_PLUGINS_CONFIGURATION_NAME = "dokkaPlugin"

        internal val jsonMapper = Json {
            prettyPrint = true
            @OptIn(ExperimentalSerializationApi::class)
            prettyPrintIndent = "  "
        }

        private val logger: Logger = Logging.getLogger(DokkaBasePlugin::class.java)

        private val minimumSupportedGradleVersion = GradleVersion.version("7.0")
    }
}
