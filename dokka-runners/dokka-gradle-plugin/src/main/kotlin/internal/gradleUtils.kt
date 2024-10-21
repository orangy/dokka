/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.api.*
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.api.services.BuildServiceSpec
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion
import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName


/**
 * Mark this [Configuration] as one that should be used to declare dependencies in
 * [Project.dependencies] block.
 *
 * Declarable Configurations should be extended by [resolvable] and [consumable] Configurations.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = false
 * isCanBeDeclared = true
 * ```
 */
internal fun Configuration.declarable(
    visible: Boolean = false,
) {
    isCanBeResolved = false
    isCanBeConsumed = false
    canBeDeclared(true)
    isVisible = visible
}


/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.consumable(
    visible: Boolean = false,
) {
    isCanBeResolved = false
    isCanBeConsumed = true
    canBeDeclared(false)
    isVisible = visible
}


/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.resolvable(
    visible: Boolean = false,
) {
    isCanBeResolved = true
    isCanBeConsumed = false
    canBeDeclared(false)
    isVisible = visible
}


/**
 * Enable/disable [Configuration.isCanBeDeclared] only if it is supported by the
 * [CurrentGradleVersion]
 *
 * This function should be removed when the minimal supported Gradle version is 8.2.
 */
@Suppress("UnstableApiUsage")
private fun Configuration.canBeDeclared(value: Boolean) {
    if (CurrentGradleVersion >= "8.2") {
        isCanBeDeclared = value
    }
}


/** Shortcut for [GradleVersion.current] */
internal val CurrentGradleVersion: GradleVersion
    get() = GradleVersion.current()


/** Compare a [GradleVersion] to a [version]. */
internal operator fun GradleVersion.compareTo(version: String): Int =
    compareTo(GradleVersion.version(version))


/** Only matches components that come from subprojects */
internal object LocalProjectOnlyFilter : Spec<ComponentIdentifier> {
    override fun isSatisfiedBy(element: ComponentIdentifier?): Boolean =
        element is ProjectComponentIdentifier
}


/** Invert the result of a [Spec] predicate */
internal operator fun <T> Spec<T>.not(): Spec<T> = Spec<T> { !this@not.isSatisfiedBy(it) }


internal fun Project.pathAsFilePath(): String = path
    .removePrefix(GradleProjectPath.SEPARATOR)
    .replace(GradleProjectPath.SEPARATOR, "/")


/**
 * Apply some configuration to a [Task] using
 * [configure][org.gradle.api.tasks.TaskContainer.configure],
 * and return the same [TaskProvider].
 */
internal fun <T : Task> TaskProvider<T>.configuring(
    block: Action<T>
): TaskProvider<T> = apply { configure(block) }


internal fun <T> NamedDomainObjectContainer<T>.maybeCreate(
    name: String,
    configure: T.() -> Unit,
): T = maybeCreate(name).apply(configure)


/**
 * Aggregate the incoming files from a [Configuration] (with name [named]) into [collector].
 *
 * Configurations that do not exist or cannot be
 * [resolved][org.gradle.api.artifacts.Configuration.isCanBeResolved]
 * will be ignored.
 *
 * @param[builtBy] An optional [TaskProvider], used to set [ConfigurableFileCollection.builtBy].
 * This should not typically be used, and is only necessary in rare cases where a Gradle Plugin is
 * misconfigured.
 */
internal fun ConfigurationContainer.collectIncomingFiles(
    named: String,
    collector: ConfigurableFileCollection,
    builtBy: TaskProvider<*>? = null,
    artifactViewConfiguration: ArtifactView.ViewConfiguration.() -> Unit = {
        // ignore failures: it's usually okay if fetching files is best-effort because
        // maybe Dokka doesn't need _all_ dependencies
        lenient(true)
    },
) {
    val conf = findByName(named)
    if (conf != null && conf.isCanBeResolved) {
        val incomingFiles = conf.incoming
            .artifactView(artifactViewConfiguration)
            .artifacts
            .resolvedArtifacts // using 'resolved' might help with triggering artifact transforms?
            .map { artifacts -> artifacts.map { it.file } }

        collector.from(incomingFiles)

        if (builtBy != null) {
            collector.builtBy(builtBy)
        }
    }
}


/**
 * Create a new [NamedDomainObjectContainer], using
 * [org.gradle.kotlin.dsl.domainObjectContainer]
 * (but [T] is `reified`).
 *
 * @param[factory] an optional factory for creating elements
 * @see org.gradle.kotlin.dsl.domainObjectContainer
 */
internal inline fun <reified T : Any> ObjectFactory.domainObjectContainer(
    factory: NamedDomainObjectFactory<T>? = null
): NamedDomainObjectContainer<T> =
    if (factory == null) {
        domainObjectContainer(T::class)
    } else {
        domainObjectContainer(T::class, factory)
    }


/**
 * Create a new [ExtensiblePolymorphicDomainObjectContainer], using
 * [org.gradle.kotlin.dsl.polymorphicDomainObjectContainer]
 * (but [T] is `reified`).
 *
 * @see org.gradle.kotlin.dsl.polymorphicDomainObjectContainer
 */
internal inline fun <reified T : Any> ObjectFactory.polymorphicDomainObjectContainer()
        : ExtensiblePolymorphicDomainObjectContainer<T> =
    polymorphicDomainObjectContainer(T::class)


/**
 * Add an extension to the [ExtensionContainer], and return the value.
 *
 * Adding an extension is especially useful for improving the DSL in build scripts when [T] is a
 * [NamedDomainObjectContainer].
 * Using an extension will allow Gradle to generate
 * [type-safe model accessors](https://docs.gradle.org/current/userguide/kotlin_dsl.html#kotdsl:accessor_applicability)
 * for added types.
 *
 * ([name] should match the property name. This has to be done manually. I tried using a
 * delegated-property provider, but then Gradle can't introspect the types properly, so it fails to
 * create accessors).
 */
internal inline fun <reified T : Any> ExtensionContainer.adding(
    name: String,
    value: T,
): T {
    add<T>(name, value)
    return value
}


/** Create a new [DokkaPluginParametersContainer] instance. */
internal fun ObjectFactory.dokkaPluginParametersContainer(): DokkaPluginParametersContainer {
    val container = polymorphicDomainObjectContainer<DokkaPluginParametersBaseSpec>()
    container.whenObjectAdded {
        // workaround for https://github.com/gradle/gradle/issues/24972
        (container as ExtensionAware).extensions.add(name, this)
    }
    return container
}


/**
 * Creates a new [Attribute] of the given name with a type of [String].
 *
 * @see Attribute.of
 */
@JvmName("StringAttribute")
internal fun Attribute(
    name: String
): Attribute<String> =
    Attribute.of(name, String::class.java)


internal val ArtifactTypeAttribute: Attribute<String> = Attribute("artifactType")


internal fun AttributeContainer.artifactType(value: String) {
    attribute(ArtifactTypeAttribute, value)
}


/**
 * Get all [Attribute]s as a [Map] (helpful for debug printing)
 */
internal fun AttributeContainer.toMap(): Map<Attribute<*>, Any?> =
    keySet().associateWith { getAttribute(it) }


internal fun AttributeContainer.toDebugString(): String =
    toMap().entries.joinToString { (k, v) -> "$k[name:${k.name}, type:${k.type}, type.hc:${k.type.hashCode()}]=$v" }


/**
 * Get an [Attribute] from an [AttributeContainer].
 *
 * (Nicer Kotlin accessor function).
 */
internal operator fun <T : Any> AttributeContainer.get(key: Attribute<T>): T? {
    // first, try the official way
    val value = getAttribute(key)
    if (value != null) {
        return value
    }

    // Failed to get attribute using official method, which might have been caused by a Gradle bug
    // https://github.com/gradle/gradle/issues/28695
    // Attempting to check...

    // Quickly check that any attribute has the same name.
    // (There's no point in checking further if no names match.)
    if (keySet().none { it.name == key.name }) {
        return null
    }

    val actualKey = keySet()
        .firstOrNull { candidate -> candidate.matchesTypeOf(key) }
        ?: return null

    error(
        """
          Gradle failed to fetch attribute from AttributeContainer, even though the attribute is present.
          Please report this error to Gradle https://github.com/gradle/gradle/issues/28695
            Requested attribute: $key ${key.type} ${key.type.hashCode()}
            Actual attribute: $actualKey ${actualKey.type} ${actualKey.type.hashCode()}
            All attributes: ${toDebugString()}
            Gradle Version: $CurrentGradleVersion
        """.trimIndent()
    )
}

/** Leniently check if [Attribute.type]s are equal, avoiding [Class.hashCode] classloader differences. */
private fun Attribute<*>.matchesTypeOf(other: Attribute<*>): Boolean {
    val thisTypeId = this.typeId() ?: false
    val otherTypeId = other.typeId() ?: false
    return thisTypeId == otherTypeId
}

/**
 * An ID for [Attribute.type] that is stable across different classloaders.
 *
 * Workaround for https://github.com/gradle/gradle/issues/28695.
 */
private fun Attribute<*>.typeId(): String? =
    type.toString().ifBlank { null }


/**
 * Registers a service, named by the [jvmName] of [serviceClass].
 *
 * @param[classLoaderScoped] If `true`, register a new service with a new name, suffixed with the value of [ClassLoader.hashCode],
 * to avoid issues related to Gradle classloaders isolation.
 *
 * See
 * - https://github.com/gradle/gradle/issues/17559
 * - https://github.com/JetBrains/kotlin/blob/96205cabfdb14a5aa5b1f0127871cee9c09aaef9/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/utils/gradleUtils.kt#L29-L35
 * @see [BuildServiceRegistry.registerIfAbsent]
 * @see [PluginFeaturesService.Params.primaryService]
 */
internal fun <T : BuildService<P>, P : BuildServiceParameters> BuildServiceRegistry.registerIfAbsent(
    serviceClass: KClass<T>,
    classLoaderScoped: Boolean = false,
    configureAction: Action<BuildServiceSpec<P>> = Action {},
): Provider<T> {
    val serviceName =
        if (classLoaderScoped) {
            "${serviceClass.jvmName}_${serviceClass.java.classLoader.hashCode()}"
        } else {
            serviceClass.jvmName
        }
    return registerIfAbsent(serviceName, serviceClass, configureAction)
}

/**
 * Suffix tag to indicate that a [Configuration] is internal, and is intended for users.
 *
 * Dokka has a lot of Configurations, so this helps with deciphering which Configuration to use in
 * a build scripts `dependencies {}` block.
 */
internal const val INTERNAL_CONF_NAME_TAG = "~internal"


internal const val INTERNAL_CONF_DESCRIPTION_TAG = "[Internal Dokka Configuration]"


/**
 * Get the root-most [Gradle] instance.
 *
 * This is useful for determining the root directory of Composite Builds.
 */
internal fun Project.rootGradle(): Gradle =
    generateSequence(gradle) { it.parent }.last()
