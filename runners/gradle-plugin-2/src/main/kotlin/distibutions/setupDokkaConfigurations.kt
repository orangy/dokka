package org.jetbrains.dokka.gradle.distibutions

//import org.gradle.api.Project
//import org.gradle.api.artifacts.Configuration
//import org.gradle.api.attributes.Attribute
//import org.gradle.api.attributes.AttributeContainer
//import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
//import org.gradle.api.attributes.Bundling.EXTERNAL
//import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
//import org.gradle.api.attributes.Category.LIBRARY
//import org.gradle.api.attributes.LibraryElements.JAR
//import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
//import org.gradle.api.attributes.Usage.JAVA_RUNTIME
//import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
//import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
//import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
//import org.gradle.kotlin.dsl.named
//import org.gradle.kotlin.dsl.newInstance
//import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName
//import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_PLUGINS_CLASSPATH
//import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.ConfigurationName.DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH
//import org.jetbrains.dokka.gradle.distibutions.DokkaPluginAttributes.Companion.DOKKA_BASE_ATTRIBUTE
//import org.jetbrains.dokka.gradle.distibutions.DokkaPluginAttributes.Companion.DOKKA_CATEGORY_ATTRIBUTE
//import org.jetbrains.dokka.gradle.distibutions.DokkaPluginAttributes.Companion.DOKKA_FORMAT_ATTRIBUTE
//import org.jetbrains.dokka.gradle.internal.asConsumer
//import org.jetbrains.dokka.gradle.internal.asProvider
//
///**
// * Initialise [DokkaPluginConfigurations].
// *
// * (Be careful of the confusing names: Gradle [Configuration]s are used to transfer files,
// * [DokkaConfiguration][org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs]
// * is used to configure Dokka behaviour.)
// */
//internal fun Project.setupDokkaConfigurations(
//    formatName: String,
//): DokkaPluginConfigurations {
//
//    val attributes = objects.newInstance<DokkaPluginAttributes>()
//
//    dependencies.attributesSchema {
//        attribute(DOKKA_BASE_ATTRIBUTE)
//        attribute(DOKKA_CATEGORY_ATTRIBUTE)
//    }
//
//    /** A general attribute for all [Configuration]s that are used by the Dokka Gradle plugin */
//    fun AttributeContainer.dokkaBaseUsage() {
//        attribute(DOKKA_BASE_ATTRIBUTE, attributes.dokkaBaseUsage)
//    }
//
//    fun AttributeContainer.dokkaCategory(category: DokkaPluginAttributes.DokkaCategory) {
//        dokkaBaseUsage()
//        attribute(DOKKA_FORMAT_ATTRIBUTE, objects.named(formatName))
//        attribute(DOKKA_CATEGORY_ATTRIBUTE, category)
//    }
//
//    fun AttributeContainer.jvmJar() {
//        attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
//        attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
//        attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
//        attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
//        attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
//
////         tell Gradle to only resolve Kotlin/JVM dependencies (might not need this)
////        attribute(kotlinPlatformType, "jvm")
//    }
//
//
//    val dokkaConsumer = configurations.maybeCreate(ConfigurationName.DOKKA).apply {
//        description = "Fetch all Dokka files from all configurations in other subprojects"
//        asConsumer()
//        isVisible = false
//        attributes { dokkaBaseUsage() }
//    }
//
//    //<editor-fold desc="Dokka Configuration files">
//    val dokkaConfigurationsConsumer =
//        configurations.register(ConfigurationName.DOKKA_CONFIGURATIONS) {
//            description = "Fetch Dokka Generator Configuration files from other subprojects"
//            asConsumer()
//            extendsFrom(dokkaConsumer)
//            isVisible = false
//            attributes {
//                dokkaCategory(attributes.dokkaConfiguration)
//            }
//        }
//
//    val dokkaConfigurationsProvider =
//        configurations.register(ConfigurationName.DOKKA_CONFIGURATION_ELEMENTS) {
//            description = "Provide Dokka Generator Configuration files to other subprojects"
//            asProvider()
//            // extend from dokkaConfigurationsConsumer, so Dokka Module Configs propagate api() style
//            extendsFrom(dokkaConfigurationsConsumer.get())
//            isVisible = true
//            attributes {
//                dokkaCategory(attributes.dokkaConfiguration)
//            }
//        }
//    //</editor-fold>
//
//
//    //<editor-fold desc="Module descriptor configurations">
//    val dokkaModuleDescriptorsConsumer =
//        configurations.register(ConfigurationName.DOKKA_MODULE_DESCRIPTORS) {
//            description = "Fetch Dokka Module descriptor files from other subprojects"
//            asConsumer()
//            isVisible = false
////            extendsFrom(dokkaConsumer.get()) // don't auto fetch, module dependencies must be declared explicitly
//            attributes {
//                dokkaCategory(attributes.dokkaModuleDescriptors)
//            }
//        }
//
//    val dokkaModuleDescriptorsProvider =
//        configurations.register(ConfigurationName.DOKKA_MODULE_DESCRIPTOR_ELEMENTS) {
//            description = "Provide Dokka Module descriptor files to other subprojects"
//            asProvider()
//            isVisible = true
//            // extend from dokkaModuleDescriptorsConsumer, so Dokka Module Configs propagate api() style
//            extendsFrom(dokkaModuleDescriptorsConsumer.get())
//            attributes {
//                dokkaCategory(attributes.dokkaModuleDescriptors)
//            }
//        }
//    //</editor-fold>
//
//
//    //<editor-fold desc="Dokka Generator Plugins">
//    val dokkaPluginsClasspath = configurations.register(DOKKA_PLUGINS_CLASSPATH) {
//        description = "Dokka Plugins classpath"
//        asConsumer()
//        extendsFrom(dokkaConsumer)
//        isVisible = true
//        attributes {
//            jvmJar()
//            dokkaCategory(attributes.dokkaPluginsClasspath)
//        }
//    }
//
//    val dokkaPluginsIntransitiveClasspath = configurations.register(DOKKA_PLUGINS_INTRANSITIVE_CLASSPATH) {
//        description =
//            "Dokka Plugins classpath - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration."
//        asConsumer()
//        extendsFrom(dokkaPluginsClasspath.get())
//        isVisible = false
//        isTransitive = false
//        attributes {
//            jvmJar()
//            dokkaCategory(attributes.dokkaPluginsClasspath)
//        }
//    }
//
//    configurations.register(DOKKA_PLUGINS_CLASSPATH + "Elements") {
//        // defining this is _required_ otherwise Gradle will use its imagination and fill in the blanks with random dependencies
//        description = "share Dokka Plugins classpath with other subprojects"
//        asProvider()
//        isVisible = true
//        extendsFrom(dokkaPluginsClasspath.get())
//        attributes {
//            jvmJar()
//            dokkaCategory(attributes.dokkaPluginsClasspath)
//        }
//    }
//    //</editor-fold>
//
//    //<editor-fold desc="Dokka Generator Classpath">
//    val dokkaGeneratorClasspath = configurations.register(ConfigurationName.DOKKA_GENERATOR_CLASSPATH) {
//        description =
//            "Dokka Generator runtime classpath - will be used in Dokka Worker. Should contain all plugins, and transitive dependencies, so Dokka Worker can run."
//        asConsumer()
//        isVisible = false
//
//        extendsFrom(dokkaConsumer)
//
//        // extend from plugins classpath, so Dokka Worker can run the plugins
//        extendsFrom(dokkaPluginsClasspath.get())
//
//        isTransitive = true
//        attributes {
//            jvmJar()
//            dokkaCategory(attributes.dokkaGeneratorClasspath)
//        }
//    }
//
//    configurations.register(ConfigurationName.DOKKA_GENERATOR_CLASSPATH + "Elements") {
//        description = "Provide Dokka Generator classpath to other subprojects"
//        asProvider()
//        isVisible = true
//        attributes {
//            jvmJar()
//            dokkaCategory(attributes.dokkaGeneratorClasspath)
//        }
//        extendsFrom(dokkaGeneratorClasspath.get())
//    }
//    //</editor-fold>
//
//    return DokkaPluginConfigurations(
//        dokkaConsumer = dokkaConsumer,
//        dokkaConfigurationsConsumer = dokkaConfigurationsConsumer,
//        dokkaModuleDescriptorsConsumer = dokkaModuleDescriptorsConsumer,
//        dokkaConfigurationsElements = dokkaConfigurationsProvider,
//        dokkaModuleDescriptorsElements = dokkaModuleDescriptorsProvider,
//        dokkaPluginsClasspath = dokkaPluginsClasspath,
//        dokkaGeneratorClasspath = dokkaGeneratorClasspath,
//        dokkaPluginsIntransitiveClasspath = dokkaPluginsIntransitiveClasspath,
//    )
//}
//
//
//
//private val kotlinPlatformType = Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)
