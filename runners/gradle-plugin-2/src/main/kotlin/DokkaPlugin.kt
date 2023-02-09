package org.jetbrains.dokka.gradle

import kotlinx.serialization.json.Json
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
import org.jetbrains.dokka.gradle.adapters.DokkaKotlinAdapter
import org.jetbrains.dokka.gradle.distibutions.DokkaPluginAttributes
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaPluginConfigurationGradleBuilder
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaPublication
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaSourceSetGradleBuilder
import org.jetbrains.dokka.gradle.formats.*
import org.jetbrains.dokka.gradle.internal.asConsumer
import org.jetbrains.dokka.gradle.internal.asProvider
import org.jetbrains.dokka.gradle.tasks.DokkaConfigurationTask
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.dokka.gradle.tasks.DokkaTask
import javax.inject.Inject


abstract class DokkaPlugin @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
    private val objects: ObjectFactory,
) : Plugin<Project> {

    override fun apply(target: Project) {

        val dokkaExtension = target.extensions.create<DokkaExtension>(EXTENSION_NAME).apply {
            dokkaVersion.convention("1.7.20")
            dokkaCacheDirectory.convention(null)
            moduleNameDefault.convention(providers.provider { target.name })
            moduleVersionDefault.convention(providers.provider { target.version.toString() })
            sourceSetScopeDefault.convention(target.path)
            dokkaPublicationDirectory.convention(layout.buildDirectory.dir("dokka"))
            dokkaConfigurationsDirectory.convention(layout.buildDirectory.dir("dokka-config"))
        }

        target.tasks.createDokkaLifecycleTasks()

        val configurationAttributes = objects.newInstance<DokkaPluginAttributes>()

        target.dependencies.attributesSchema {
            attribute(DokkaPluginAttributes.DOKKA_BASE_ATTRIBUTE)
            attribute(DokkaPluginAttributes.DOKKA_CATEGORY_ATTRIBUTE)
            attribute(DokkaPluginAttributes.DOKKA_FORMAT_ATTRIBUTE)
        }

        val dokkaConsumerConfiguration = target.configurations.register(ConfigurationName.DOKKA) {
            description = "Fetch all Dokka files from all configurations in other subprojects"
            asConsumer()
            isVisible = false
            attributes {
                attribute(DokkaPluginAttributes.DOKKA_BASE_ATTRIBUTE, configurationAttributes.dokkaBaseUsage)
            }
        }

        configureAllDokkaPublications(
            target,
            dokkaExtension,
            dokkaConsumerConfiguration,
            configurationAttributes,
        )

        configureDokkaPublicationsDefaults(
            dokkaExtension = dokkaExtension,
        )


        target.tasks.withType<DokkaGenerateTask>().configureEach {
            cacheDirectory.convention(dokkaExtension.dokkaCacheDirectory)
        }

//        target.tasks.withType<DokkaConfigurationTask>().configureEach {
//        }


        // apply the plugin that will autoconfigure Dokka to use the sources of a Kotlin project
        with(target.pluginManager) {
            apply(type = DokkaKotlinAdapter::class)

            // auto-apply the custom format plugins
            apply(type = DokkaGfmPublicationPlugin::class)
            apply(type = DokkaHtmlPublicationPlugin::class)
            apply(type = DokkaJavadocPublicationPlugin::class)
            apply(type = DokkaJekyllPublicationPlugin::class)
        }
    }


    /** proactive create the tasks and configurations required for generating each Dokka Publication */
    private fun configureAllDokkaPublications(
        project: Project,
        dokkaExtension: DokkaExtension,
        dokkaConsumerConfiguration: NamedDomainObjectProvider<Configuration>,
        configurationAttributes: DokkaPluginAttributes,
    ) {
        dokkaExtension.dokkaPublications.all publication@{

            // create Gradle Configurations
            val gradleConfigurations = createDokkaFormatConfigurations(
                dokkaConsumerConfiguration,
                objects,
                configurationAttributes,
                project.configurations,
            )

            // add default Configuration dependencies
            project.dependencies.addDokkaDependencies(
                dokkaExtension,
                gradleConfigurations,
            )

            // create tasks
            val createConfigurationTask = project.tasks.register<DokkaConfigurationTask>(
                taskNames.createConfiguration
            ) {
                description =
                    "Creates Dokka Configuration for executing the Dokka Generator for the $formatName publication"

                dokkaConfigurationJson.convention(dokkaExtension.dokkaConfigurationsDirectory.file("$formatName/dokka_configuration.json"))

                // depend on Dokka Configurations from other subprojects
                dokkaSubprojectConfigurations.from(
                    gradleConfigurations.dokkaConfigurationsConsumer.map { elements ->
                        elements.incoming.artifactView { lenient(true) }.files
                    }
                )

                //// depend on Dokka Module Configurations from other subprojects
                //dokkaModuleDescriptorFiles.from(
                //    gradleConfigurations.dokkaModuleDescriptorsConsumer.map { elements ->
                //        elements.incoming.artifactView { lenient(true) }.files
                //    }
                //)

                publicationEnabled.convention(this@publication.enabled)
                onlyIf { publicationEnabled.getOrElse(true) }

                cacheRoot.set(dokkaConfiguration.cacheRoot)
                delayTemplateSubstitution.set(dokkaConfiguration.delayTemplateSubstitution)
                dokkaSourceSets.addAllLater(providers.provider { this@publication.dokkaConfiguration.dokkaSourceSets })
                failOnWarning.set(dokkaConfiguration.failOnWarning)
                finalizeCoroutines.set(dokkaConfiguration.finalizeCoroutines)
                includes.from(dokkaConfiguration.includes)
                moduleName.set(dokkaConfiguration.moduleName)
                moduleVersion.set(dokkaConfiguration.moduleVersion)
                offlineMode.set(dokkaConfiguration.offlineMode)
                outputDir.set(dokkaConfiguration.outputDir)
                pluginsClasspath.from(gradleConfigurations.dokkaPluginsIntransitiveClasspath)

                pluginsConfiguration.addAllLater(providers.provider { dokkaConfiguration.pluginsConfiguration })

                //<editor-fold desc="adapter for old DSL - to be removed">
                pluginsConfiguration.addAllLater(
                    @Suppress("DEPRECATION")
                    pluginsMapConfiguration.map { pluginConfig ->
                        pluginConfig.map { (pluginId, pluginConfiguration) ->
                            objects.newInstance<DokkaPluginConfigurationGradleBuilder>().apply {
                                fqPluginName.set(pluginId)
                                values.set(pluginConfiguration)
                            }
                        }
                    }
                )
                pluginsConfiguration.configureEach {
                    serializationFormat.convention(DokkaConfiguration.SerializationFormat.JSON)
                }
                //</editor-fold>

                suppressInheritedMembers.set(dokkaConfiguration.suppressInheritedMembers)
                suppressObviousFunctions.set(dokkaConfiguration.suppressObviousFunctions)

                dokkaSourceSets.configureEach {
                    // TODO for some reason the conventions need to be set again
                    analysisPlatform.convention(Platform.DEFAULT)
                    displayName.convention("main")
                    documentedVisibilities.convention(listOf(DokkaConfiguration.Visibility.PUBLIC))
                    jdkVersion.convention(8)
                    noAndroidSdkLink.convention(false)
                    noJdkLink.convention(false)
                    noStdlibLink.convention(false)
                    reportUndocumented.convention(false)
                    skipDeprecated.convention(false)
                    skipEmptyPackages.convention(true)
                    sourceSetScope.convention(dokkaExtension.sourceSetScopeDefault)
                    //suppress.convention(false) // TODO need to re-enable suppress convention, it's only disabled so the hack workaround 'todoSourceSetName' works
                    suppressGeneratedFiles.convention(true)

                    sourceLinks.configureEach {
                        localDirectory.convention(layout.projectDirectory.asFile)
                        remoteLineSuffix.convention("#L")
                    }

                    perPackageOptions.configureEach {
                        matchingRegex.convention(".*")
                        suppress.convention(false)
                        skipDeprecated.convention(false)
                        reportUndocumented.convention(false)
                        @Suppress("DEPRECATION")
                        includeNonPublic.convention(false)
                    }
                }
            }

            project.tasks.register<DokkaGenerateTask>(taskNames.generate) {
                description = "Executes the Dokka Generator, producing the $formatName publication"
                outputDirectory.convention(dokkaExtension.dokkaPublicationDirectory.dir(formatName))
                dokkaConfigurationJson.convention(createConfigurationTask.flatMap { it.dokkaConfigurationJson })
                runtimeClasspath.from(gradleConfigurations.dokkaGeneratorClasspath)
            }

            gradleConfigurations.dokkaConfigurationsElements.configure {
                outgoing {
                    artifact(createConfigurationTask.flatMap { it.dokkaConfigurationJson })
                }
            }
        }
    }

    /** Set defaults in all [org.jetbrains.dokka.gradle.dokka_configuration.DokkaPublication]s */
    private fun configureDokkaPublicationsDefaults(
        dokkaExtension: DokkaExtension,
    ) {

        fun DokkaSourceSetGradleBuilder.configureDefaults() {
            analysisPlatform.convention(Platform.DEFAULT)
            displayName.convention("main")
            documentedVisibilities.convention(listOf(DokkaConfiguration.Visibility.PUBLIC))
            jdkVersion.convention(8)
            noAndroidSdkLink.convention(false)
            noJdkLink.convention(false)
            noStdlibLink.convention(false)
            reportUndocumented.convention(false)
            skipDeprecated.convention(false)
            skipEmptyPackages.convention(true)
            sourceSetScope.convention(dokkaExtension.sourceSetScopeDefault)
            //suppress.convention(false) // TODO need to re-enable suppress convention, it's only disabled so the hack workaround 'todoSourceSetName' works
            suppressGeneratedFiles.convention(true)

            sourceLinks.configureEach {
                localDirectory.convention(layout.projectDirectory.asFile)
                remoteLineSuffix.convention("#L")
            }

            perPackageOptions.configureEach {
                matchingRegex.convention(".*")
                suppress.convention(false)
                skipDeprecated.convention(false)
                reportUndocumented.convention(false)
                @Suppress("DEPRECATION")
                includeNonPublic.convention(false)
            }
        }

        dokkaExtension.dokkaSourceSets.configureEach {
            configureDefaults()
        }
        dokkaExtension.dokkaPublications.configureEach {

            enabled.convention(true)

            dokkaConfiguration.apply {

                cacheRoot.convention(dokkaExtension.dokkaCacheDirectory)
                delayTemplateSubstitution.convention(false)
                failOnWarning.convention(false)
                finalizeCoroutines.convention(false)
                moduleName.convention(dokkaExtension.moduleNameDefault)
                moduleVersion.convention(dokkaExtension.moduleVersionDefault)
                offlineMode.convention(false)
                outputDir.convention(dokkaExtension.dokkaPublicationDirectory)
                suppressInheritedMembers.convention(false)
                suppressObviousFunctions.convention(true)

                // 'inherit' the common source sets defined in the extension
                dokkaSourceSets.addAllLater(
                    objects.listProperty<DokkaSourceSetGradleBuilder>().apply {
                        addAll(
                            providers.provider { dokkaExtension.dokkaSourceSets }
                        )
                    }
                )

                // and configure each source set with the defaults
                dokkaSourceSets.configureEach {
                    configureDefaults()
                }

                pluginsConfiguration.configureEach {
                    serializationFormat.convention(DokkaConfiguration.SerializationFormat.JSON)
                }
            }
        }
    }

    // I don't think Dokka Modules are necessary - but they might make a return
//    private fun TaskContainer.registerDokkaModuleConfigurationTask(
//        dokkaGenerateTask: TaskProvider<DokkaGenerateTask>,
//    ): TaskProvider<DokkaModuleConfigurationTask> {
//        val dokkaModuleConfigurationTask =
//            register<DokkaModuleConfigurationTask>(TaskName.CREATE_DOKKA_MODULE_CONFIGURATION)
//
//        withType<DokkaModuleConfigurationTask>().configureEach {
//            moduleName.set(project.name.map { it.takeIf(Char::isLetterOrDigit) ?: "-" }.joinToString(""))
//            dokkaModuleConfigurationJson.set(
//                moduleName.flatMap { moduleName ->
//                    layout.buildDirectory.file("dokka/$moduleName.json")
//                }
//            )
//
//            dependsOn(dokkaGenerateTask)
//
////            moduleOutputDirectoryPath(dokkaGenerateTask.map { it.outputDirectory })
//            sourceOutputDirectory(dokkaGenerateTask.map { it.outputDirectory })
////            sourceOutputDirectory(layout.buildDirectory.dir("dokka/source-output"))
//        }
//
//        return dokkaModuleConfigurationTask
//    }

    /**
     * Create [DokkaFormatGradleConfigurations].
     *
     * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
     * [DokkaConfiguration][org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs]
     * is used to configure Dokka behaviour.)
     */
    private fun DokkaPublication.createDokkaFormatConfigurations(
        dokkaConsumer: NamedDomainObjectProvider<Configuration>,
        objects: ObjectFactory,
        attributes: DokkaPluginAttributes,
        configurations: ConfigurationContainer,
    ): DokkaFormatGradleConfigurations {

        fun AttributeContainer.dokkaCategory(category: DokkaPluginAttributes.DokkaCategory) {
            attribute(DokkaPluginAttributes.DOKKA_BASE_ATTRIBUTE, attributes.dokkaBaseUsage)
            attribute(DokkaPluginAttributes.DOKKA_FORMAT_ATTRIBUTE, objects.named(formatName))
            attribute(DokkaPluginAttributes.DOKKA_CATEGORY_ATTRIBUTE, category)
        }

        fun AttributeContainer.jvmJar() {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment.STANDARD_JVM)
            )
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))

            // tell Gradle to only resolve Kotlin/JVM dependencies (might not need this)
            //attribute(kotlinPlatformType, "jvm")
        }

        //<editor-fold desc="Dokka Configuration files">
        val dokkaConfigurationsConsumer =
            configurations.register(configurationNames.dokkaConfigurations) {
                description =
                    "Fetch Dokka Generator Configuration files for $formatName from other subprojects"
                asConsumer()
                extendsFrom(dokkaConsumer.get())
                isVisible = false
                attributes {
                    dokkaCategory(attributes.dokkaConfiguration)
                }
            }

        val dokkaConfigurationsProvider =
            configurations.register(configurationNames.dokkaConfigurationElements) {
                description =
                    "Provide Dokka Generator Configuration files for $formatName to other subprojects"
                asProvider()
                // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
                extendsFrom(dokkaConfigurationsConsumer.get())
                isVisible = true
                attributes {
                    dokkaCategory(attributes.dokkaConfiguration)
                }
            }
        //</editor-fold>

        //<editor-fold desc="Dokka Generator Plugins">
        val dokkaPluginsClasspath =
            configurations.register(configurationNames.dokkaPluginsClasspath) {
                description = "Dokka Plugins classpath for $formatName"
                asConsumer()
                isVisible = false
                attributes {
                    jvmJar()
                    dokkaCategory(attributes.dokkaPluginsClasspath)
                }
            }

        val dokkaPluginsIntransitiveClasspath =
            configurations.register(configurationNames.dokkaPluginsIntransitiveClasspath) {
                description =
                    "Dokka Plugins classpath for $formatName - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
                asConsumer()
                extendsFrom(dokkaPluginsClasspath.get())
                isVisible = false
                isTransitive = false
                attributes {
                    jvmJar()
                    dokkaCategory(attributes.dokkaPluginsClasspath)
                }
            }
        //</editor-fold>

        //<editor-fold desc="Dokka Generator Classpath">
        val dokkaGeneratorClasspath =
            configurations.register(configurationNames.dokkaGeneratorClasspath) {
                description =
                    "Dokka Generator runtime classpath for $formatName - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run."
                asConsumer()
                isVisible = false

                // also receive the classpath from other subprojects
//                extendsFrom(dokkaConsumer.get())

                // extend from plugins classpath, so Dokka Worker can run the plugins
                extendsFrom(dokkaPluginsClasspath.get())

                isTransitive = true
                attributes {
                    jvmJar()
                    dokkaCategory(attributes.dokkaGeneratorClasspath)
                }
            }

//        configurations.register(configurationNames.dokkaGeneratorClasspath + "Elements") {
//            description = "Provide Dokka Generator classpath to other subprojects"
//            asProvider()
//            isVisible = true
//            attributes {
//                jvmJar()
//                dokkaCategory(attributes.dokkaGeneratorClasspath)
//            }
//            extendsFrom(dokkaGeneratorClasspath.get())
//        }
        //</editor-fold>

        return DokkaFormatGradleConfigurations(
//            dokkaConsumer = dokkaConsumer,
            dokkaConfigurationsConsumer = dokkaConfigurationsConsumer,
            dokkaConfigurationsElements = dokkaConfigurationsProvider,
            dokkaPluginsClasspath = dokkaPluginsClasspath,
            dokkaGeneratorClasspath = dokkaGeneratorClasspath,
            dokkaPluginsIntransitiveClasspath = dokkaPluginsIntransitiveClasspath,
        )
    }


    private fun DependencyHandler.addDokkaDependencies(
        dokkaExtension: DokkaExtension,
        formatConfigurations: DokkaFormatGradleConfigurations,
    ) {

        //<editor-fold desc="DependencyHandler utils">
        fun DependencyHandler.dokkaPlugin(dependency: Provider<Dependency>) =
            addProvider(formatConfigurations.dokkaPluginsClasspath.name, dependency)

        fun DependencyHandler.dokkaPlugin(dependency: String) =
            add(formatConfigurations.dokkaPluginsClasspath.name, dependency)

        fun DependencyHandler.dokkaGenerator(dependency: Provider<Dependency>) =
            addProvider(formatConfigurations.dokkaGeneratorClasspath.name, dependency)

        fun DependencyHandler.dokkaGenerator(dependency: String) =
            add(formatConfigurations.dokkaGeneratorClasspath.name, dependency)
        //</editor-fold>

        fun dokka(module: String) =
            dokkaExtension.dokkaVersion.map { version -> create("org.jetbrains.dokka:$module:$version") }

        dokkaPlugin("org.jetbrains:markdown-jvm:0.3.1")
        dokkaPlugin(dokka("kotlin-analysis-intellij"))
        dokkaPlugin(dokka("dokka-base"))
        dokkaPlugin(dokka("templating-plugin"))
        dokkaPlugin(dokka("dokka-analysis"))
        dokkaPlugin(dokka("kotlin-analysis-compiler"))

        dokkaPlugin("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")
        dokkaPlugin("org.freemarker:freemarker:2.3.31")

        dokkaGenerator(dokka("dokka-core"))
    }

    private fun TaskContainer.createDokkaLifecycleTasks() {
        register(TaskName.DOKKA_GENERATE, DokkaTask::class) {
            description = "Runs all Dokka Generate tasks"
            dependsOn(withType<DokkaGenerateTask>())
        }

        register(TaskName.CREATE_DOKKA_CONFIGURATION, DokkaTask::class) {
            description = "Runs all Dokka Create Configuration tasks"
            dependsOn(withType<DokkaConfigurationTask>())
        }
    }

    companion object {

        const val EXTENSION_NAME = "dokka"

        /**
         * Names of the Gradle [Configuration]s used by the Dokka Plugin.
         *
         * Beware the confusing terminology:
         * - [Gradle Configurations][org.gradle.api.artifacts.Configuration] are used to share files between subprojects. Each has a name.
         * - [Dokka Configurations][org.jetbrains.dokka.DokkaConfiguration] are used to create JSON settings for the Dokka Generator
         */
        object ConfigurationName {

            const val DOKKA = "dokka"

            /** Name of the [Configuration] that _consumes_ [org.jetbrains.dokka.DokkaConfiguration] from projects */
            const val DOKKA_CONFIGURATIONS = "dokkaConfiguration"

            /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration] to other projects */
            const val DOKKA_CONFIGURATION_ELEMENTS = "dokkaConfigurationElements"

            /** Name of the [Configuration] that _consumes_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] from projects */
            const val DOKKA_MODULE_DESCRIPTORS = "dokkaModule"

            /** Name of the [Configuration] that _provides_ [org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription] to other projects */
            const val DOKKA_MODULE_DESCRIPTOR_ELEMENTS = "dokkaModuleDescriptors"

            /**
             * Classpath used to execute the Dokka Generator.
             *
             * Extends [DOKKA_PLUGINS_CLASSPATH], so Dokka plugins and their dependencies are included.
             */
            const val DOKKA_GENERATOR_CLASSPATH = "dokkaGeneratorClasspath"

            /** Dokka Plugins (transitive, so this can be passed to the Dokka Generator Worker classpath) */
            const val DOKKA_PLUGINS_CLASSPATH = "dokkaPlugin"

            /**
             * Dokka Plugins (excluding transitive dependencies, so this be used to create Dokka Generator Configuration
             *
             * Generally, this configuration should not be invoked manually. Instead, use [DOKKA_PLUGINS_CLASSPATH].
             */
            internal const val DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH = "${DOKKA_PLUGINS_CLASSPATH}Intransitive"
        }

        /**
         * The group of all Dokka Gradle tasks.
         *
         * @see org.gradle.api.Task.getGroup
         */
        const val TASK_GROUP = "dokka"

        object TaskName {
            const val DOKKA_GENERATE = "dokkaGenerate"
            const val CREATE_DOKKA_CONFIGURATION = "createDokkaConfiguration"
            const val CREATE_DOKKA_MODULE_CONFIGURATION = "createDokkaModuleConfiguration"
        }

        internal val jsonMapper = Json {
            prettyPrint = true
        }
    }
}
