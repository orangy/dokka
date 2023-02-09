package org.jetbrains.dokka.gradle.distibutions

import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named
import javax.inject.Inject

/**
 * Gradle Configuration Attributes for Dokka files.
 *
 * These attributes are used to tag [Configuration]s, so files can be shared between subprojects.
 */
abstract class DokkaPluginAttributes @Inject constructor( // TODO rename to DokkaConfigurationAttributes
    objects: ObjectFactory,
) {

    /** A general attribute for all [Configuration]s that are used by the Dokka Gradle plugin */
//    val dokkaBaseUsage: Usage = objects.named("org.jetbrains.dokka")
    val dokkaBaseUsage: DokkaBase = objects.named("dokka")

    /** for [Configuration]s that provide or consume Dokka configuration files */
    val dokkaConfiguration: DokkaCategory = objects.named("configuration")

    /** for [Configuration]s that provide or consume Dokka module descriptor files */
    val dokkaModuleDescriptors: DokkaCategory = objects.named("module-descriptor")

    val dokkaGeneratorClasspath: DokkaCategory = objects.named("generator-classpath")

    val dokkaPluginsClasspath: DokkaCategory = objects.named("plugins-classpath")

    interface DokkaBase : Usage

    interface DokkaCategory : Named

    interface DokkaFormat : Named

    companion object {
        val DOKKA_BASE_ATTRIBUTE = Attribute.of("org.jetbrains.dokka.base", DokkaBase::class.java)
        val DOKKA_CATEGORY_ATTRIBUTE = Attribute.of("org.jetbrains.dokka.category", DokkaCategory::class.java)
        val DOKKA_FORMAT_ATTRIBUTE = Attribute.of("org.jetbrains.dokka.format", DokkaCategory::class.java)
    }
}
