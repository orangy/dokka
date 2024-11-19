/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.dokka.gradle.dependencies.BaseDependencyManager
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.formats.DokkaPublication
import org.jetbrains.dokka.gradle.internal.*
import org.jetbrains.dokka.gradle.workers.ClassLoaderIsolation
import org.jetbrains.dokka.gradle.workers.ProcessIsolation
import org.jetbrains.dokka.gradle.workers.WorkerIsolation
import java.io.Serializable
import kotlin.DeprecationLevel.ERROR

/**
 * Configure the behaviour of the [DokkaBasePlugin].
 */
abstract class DokkaExtension
@InternalDokkaGradlePluginApi
constructor(
    private val objects: ObjectFactory,
    internal val baseDependencyManager: BaseDependencyManager,
) : ExtensionAware, Serializable {

    /**
     * Base directory into which all [DokkaPublication]s will be produced.
     * By default, Dokka will generate all [DokkaPublication]s into a subdirectory inside [basePublicationsDirectory].
     *
     * To configure the output for a specific Publication, instead use [DokkaPublication.outputDirectory].
     *
     * #### Example
     *
     * Here we configure the output directory to be `./build/dokka-docs/`.
     * Dokka will produce the HTML Publication into `./build/dokka-docs/html/`
     * ```
     * dokka {
     *     basePublicationsDirectory.set(layout.buildDirectory.dir("dokka-docs"))
     * }
     * ```
     */
    abstract val basePublicationsDirectory: DirectoryProperty

    /**
     * Directory into which Dokka Modules will be produced.
     *
     * Note that Dokka Modules are intermediate products and must be combined into a completed
     * Dokka Publication. They are not intended to be comprehensible in isolation.
     */
    internal abstract val baseModulesDirectory: DirectoryProperty

    /** Default Dokka Gradle Plugin cache directory */
    abstract val dokkaCacheDirectory: DirectoryProperty

    /**
     * The display name used to refer to the module.
     * It is used for the table of contents, navigation, logging, etc.
     *
     * Default: the [current project name][org.gradle.api.Project.name].
     */
    abstract val moduleName: Property<String>

    /**
     * The displayed module version.
     *
     * Default: the [version of the current project][org.gradle.api.Project.version].
     */
    abstract val moduleVersion: Property<String>

    /**
     * Control the subdirectory used for files when aggregating this project as a Dokka Module into a Dokka Publication.
     *
     * When Dokka performs aggregation the files from each Module must be placed into separate
     * subdirectories, within the Publication directory.
     * The subdirectory used for this project's Module can be specified with this property.
     *
     * Overriding this value can be useful for fine-grained control.
     * - Setting an explicit path can help ensure that external hyperlinks to the Publication are stable,
     *   regardless of how the current Gradle project is structured.
     * - The path can also be made more specific, which is useful for
     *   [Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html),
     *   which can be more likely to cause path clashes.
     *   (The default value is distinct for a single Gradle build. With composite builds the project paths may not be distinct.)
     *   See the [Composite Build Example](https://kotl.in/dokka/examples/gradle-composite-build).
     *
     * **Important:** Care must be taken to make sure multiple Dokka Modules do not have the same paths.
     * If paths overlap then Dokka could overwrite the Modules files during aggregation,
     * resulting in a corrupted Publication.
     *
     * Default: the current project's [path][org.gradle.api.Project.getPath] as a file path.
     */
    abstract val modulePath: Property<String>

    /**
     * An arbitrary string used to group source sets that originate from different Gradle subprojects.
     *
     * This is primarily used by Kotlin Multiplatform projects, which can have multiple source sets
     * per subproject.
     *
     * Defaults to [the Gradle path of the subproject][org.gradle.api.Project.getPath].
     */
    abstract val sourceSetScopeDefault: Property<String>

    /**
     * The Konan home directory, which contains libraries for Kotlin/Native development.
     *
     * This is only required as a workaround to fetch the compile-time dependencies in Kotlin/Native
     * projects with a version below 2.0.
     */
    // This property should be removed when Dokka only supports KGP 2 or higher.
    @InternalDokkaGradlePluginApi
    abstract val konanHome: RegularFileProperty

    /**
     * The container for all [DokkaPublication]s in the current project.
     *
     * Each Dokka Publication will generate one complete Dokka site,
     * aggregated from one or more Dokka Modules.
     *
     * The type of site is determined by the Dokka Plugins. By default, an HTML site will be generated.
     *
     * #### Configuration
     *
     * To configure a specific Dokka Publications, select it by name:
     *
     * ```
     * dokka {
     *   dokkaPublications.named("html") {
     *     // ...
     *   }
     * }
     * ```
     *
     * All configurations can be configured using `.configureEach {}`:
     *
     * ```
     * dokka {
     *   dokkaPublications.configureEach {
     *     // ...
     *   }
     * }
     * ```
     */
    val dokkaPublications: NamedDomainObjectContainer<DokkaPublication> =
        extensions.adding(
            "dokkaPublications",
            objects.domainObjectContainer { named -> objects.newInstance(named, pluginsConfiguration) }
        )

    /**
     * The container for all [DokkaSourceSet][DokkaSourceSetSpec]s in the current project.
     *
     * Each `DokkaSourceSet` is analogous to a [SourceSet][org.gradle.api.tasks.SourceSet],
     * and specifies how Dokka will convert the project's source code into documentation.
     *
     * Dokka will automatically discover the current source sets in the project and create
     * a `DokkaSourceSet` for each. For example, in a Kotlin Multiplatform project Dokka
     * will create `DokkaSourceSet`s for `commonMain`, `jvmMain` etc.
     *
     * Dokka will not generate documentation unless there is at least one Dokka Source Set.
     *
     * #### Configuration
     *
     * To configure a specific Dokka Source Set, select it by name:
     *
     * ```
     * dokka {
     *   dokkaSourceSets.named("commonMain") {
     *     // ...
     *   }
     * }
     * ```
     *
     * All Source Sets can be configured using `.configureEach {}`:
     *
     * ```
     * dokka {
     *   dokkaSourceSets.configureEach {
     *     // ...
     *   }
     * }
     * ```
     */
    val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetSpec> =
        extensions.adding("dokkaSourceSets", objects.domainObjectContainer())

    /**
     * Dokka Plugin are used to configure the way Dokka generates a format.
     * Some plugins can be configured via parameters, and those parameters are stored in this
     * container.
     */
    val pluginsConfiguration: DokkaPluginParametersContainer =
        extensions.adding("pluginsConfiguration", objects.dokkaPluginParametersContainer())

    /**
     * The default version of Dokka dependencies that are used at runtime during generation.
     *
     * This value defaults to the current Dokka Gradle Plugin version, but can be overridden
     * if you want to use a newer or older version of Dokka at runtime.
     */
    abstract val dokkaEngineVersion: Property<String>

    /**
     * Dokka Gradle Plugin runs Dokka Generator in a separate
     * [Gradle Worker](https://docs.gradle.org/8.10/userguide/worker_api.html).
     *
     * DGP uses a Worker to ensure that the Java classpath required by Dokka Generator
     * is kept separate from the Gradle buildscript classpath, ensuring that dependencies
     * required for running Gradle builds don't interfere with those needed to run Dokka.
     *
     * #### Worker modes
     *
     * DGP can launch the Generator in one of two Worker modes.
     *
     * The Worker modes are used to optimise the performance of a Gradle build,
     * especially concerning the memory requirements.
     *
     * ##### [ProcessIsolation]
     *
     * The maximum isolation level. Dokka Generator is executed in a separate Java process,
     * managed by Gradle.
     *
     * The Java process parameters (such as JVM args and system properties) can be configured precisely,
     * and independently of other Gradle processes.
     *
     * Process isolation is best suited for projects where Dokka requires a lot more, or less,
     * memory than other Gradle tasks that are run more frequently.
     * This is usually the case for smaller projects, or those with default or low
     * [Gradle Daemon](https://docs.gradle.org/8.10/userguide/gradle_daemon.html)
     * memory settings.
     *
     * ##### [ClassLoaderIsolation]
     *
     * Dokka Generator is run in the current Gradle Daemon process, in a new thread with an isolated classpath.
     *
     * Classloader isolation is best suited for projects that already have high Gradle Daemon memory requirements.
     * This is usually the case for very large projects, especially Kotlin Multiplatform projects.
     * These projects will typically also require a lot of memory to running Dokka Generator.
     *
     * If the Gradle Daemon already uses a large amount of memory, it is beneficial to run Dokka Generator
     * in the same Daemon process. Running Dokka Generator inside the Daemon avoids launching
     * two Java processes on the same machine, both with high memory requirements.
     *
     * #### Example configuration
     *
     * ```kotlin
     * dokka {
     *   // use the current Gradle process, but with an isolated classpath
     *   dokkaGeneratorIsolation = ClassLoaderIsolation()
     *
     *   // launch a new process, optionally controlling the standard JVM options
     *   dokkaGeneratorIsolation = ProcessIsolation {
     *     maxHeapSize = "2g" // increase maximum heap size
     *     systemProperties.add("someCustomProperty", 123)
     *   }
     * }
     * ```
     *
     * @see WorkerIsolation
     * @see org.jetbrains.dokka.gradle.workers.ProcessIsolation
     * @see org.jetbrains.dokka.gradle.workers.ClassLoaderIsolation
     */
    // Aside: Launching without isolation WorkerExecutor.noIsolation is not an option, because
    // running Dokka Generator **requires** an isolated classpath.
    @get:Nested
    abstract val dokkaGeneratorIsolation: Property<WorkerIsolation>

    /**
     * Create a new [ClassLoaderIsolation] options instance.
     *
     * The resulting options must be set into [dokkaGeneratorIsolation].
     *
     * @see dokkaGeneratorIsolation
     */
    fun ClassLoaderIsolation(configure: ClassLoaderIsolation.() -> Unit = {}): ClassLoaderIsolation =
        objects.newInstance<ClassLoaderIsolation>().apply(configure)

    /**
     * Create a new [ProcessIsolation] options.
     *
     * The resulting options instance must be set into [dokkaGeneratorIsolation].
     *
     * @see dokkaGeneratorIsolation
     */
    fun ProcessIsolation(configure: ProcessIsolation.() -> Unit = {}): ProcessIsolation =
        objects.newInstance<ProcessIsolation>().apply(configure)


    //region deprecated properties
    /** Deprecated. Use [basePublicationsDirectory] instead. */
    // Deprecated in 2.0.0-Beta. Remove when Dokka 2.0.0 is released.
    @Deprecated(
        "Renamed to basePublicationsDirectory",
        ReplaceWith("basePublicationsDirectory"),
        level = ERROR,
    )
    @Suppress("unused")
    val dokkaPublicationDirectory: DirectoryProperty
        get() = basePublicationsDirectory

    /**
     * This property has moved to be configured on each [DokkaPublication].
     *
     * ```
     * dokka {
     *   // DEPRECATED
     *   suppressInheritedMembers.set(true)
     *
     *   // Replace with:
     *   dokkaPublications.configureEach {
     *     suppressInheritedMembers.set(true)
     *   }
     * }
     * ```
     * @see DokkaPublication.suppressInheritedMembers
     */
    @Deprecated("Moved to DokkaPublication#suppressInheritedMembers")
    abstract val suppressInheritedMembers: Property<Boolean>

    /**
     *
     * This property has moved to be configured on each [DokkaPublication].
     *
     * ```
     * dokka {
     *   // DEPRECATED
     *   suppressObviousFunctions.set(true)
     *
     *   // Replace with:
     *   dokkaPublications.configureEach {
     *     suppressObviousFunctions.set(true)
     *   }
     * }
     * ```
     *
     * @see DokkaPublication.suppressObviousFunctions
     */
    @Deprecated("Moved to DokkaPublication#suppressObviousFunctions")
    abstract val suppressObviousFunctions: Property<Boolean>

    /**
     * JSON configuration of Dokka plugins is deprecated.
     * Typesafe configuration must be used instead - see [pluginsConfiguration].
     *
     * In DPGv1 the Dokka plugins could be configured by manually writing JSON.
     * This caused issues with registering task inputs for Gradle up-to-date checks.
     * (For more information on registering task inputs, see
     * [Gradle Docs: Incremental build](https://docs.gradle.org/current/userguide/incremental_build.html)).
     *
     * In DGPv2 Dokka plugins must be configured in a typesafe way, using [pluginsConfiguration].
     *
     * #### Configuration of built-in Dokka plugins
     *
     * The built-in Dokka plugins can be configured using a typesafe DSL.
     *
     * This example demonstrates how to convert JSON configuration of the
     * [Dokka Versioning plugin](https://kotl.in/dokka-versioning-plugin)
     * into the new, typesafe config.
     *
     * ```
     * // Deprecated configuration of the Dokka Versioning plugin:
     * tasks.dokkaHtmlMultiModule {
     *     pluginsMapConfiguration.set(
     *         mapOf(
     *             "org.jetbrains.dokka.versioning.VersioningPlugin" to """
     *                 { "version": "1.2", "olderVersionsDir": "$projectDir/dokka-docs" }
     *             """.trimIndent()
     *         )
     *     )
     * }
     *
     * // New configuration in DGPv2 is typesafe and compatible with incremental builds.
     * dokka {
     *     pluginsConfiguration {
     *         versioning {
     *             version.set("1.2")
     *             olderVersionsDir.set(projectDir.resolve("dokka-docs"))
     *         }
     *     }
     * }
     * ```
     *
     * #### External Dokka Plugin configuration
     *
     * To configure external Dokka plugins you must create a subclass of
     * [DokkaPluginParametersBaseSpec][org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec],
     * and register it as a configuration type using
     * [pluginsConfiguration.registerBinding][org.gradle.api.ExtensiblePolymorphicDomainObjectContainer.registerBinding].
     *
     * ```
     * import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
     * import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
     *
     * @OptIn(InternalDokkaGradlePluginApi::class)
     * abstract class MyCustomDokkaPluginConfiguration @Inject constructor(
     *     name: String
     * ) : DokkaPluginParametersBaseSpec(name, "demo.MyCustomDokkaPlugin") {
     *
     *     @get:Input
     *     @get:Optional
     *     abstract val flags: ListProperty<String>
     *
     *     override fun jsonEncode(): String {
     *         // convert the 'flags' to JSON, to be decoded by MyCustomDokkaPlugin.
     *     }
     * }
     *
     * dokka {
     *     pluginsConfiguration {
     *         registerBinding(MyCustomDokkaPluginConfiguration::class, MyCustomDokkaPluginConfiguration::class)
     *         register<MyCustomDokkaPluginConfiguration>("MyCustomDokkaPlugin") {
     *             flags.add("someFlag...")
     *         }
     *     }
     * }
     * ```
     *
     * @see pluginsConfiguration
     */
    @Deprecated(
        message = "JSON configuration of Dokka plugins is deprecated. Typesafe configuration must be used instead.",
        level = DeprecationLevel.ERROR,
    )
    @Suppress("unused")
    abstract val pluginsMapConfiguration: MapProperty<String, String>
    //endregion
}
