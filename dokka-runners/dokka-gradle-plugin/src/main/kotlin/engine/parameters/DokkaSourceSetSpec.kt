/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.java
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.dokka.gradle.engine.parameters.SourceSetIdSpec.Companion.dokkaSourceSetIdSpec
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.adding
import org.jetbrains.dokka.gradle.internal.domainObjectContainer
import java.io.Serializable
import javax.inject.Inject
import kotlin.DeprecationLevel.ERROR

/**
 * A [DokkaSourceSetSpec] controls how Dokka will view and rendered sources.
 *
 * Dokka will automatically discover source sets from Kotlin, Android, and Java projects.
 *
 * Source sets can be configured individually, or all at once.
 *
 * ```kotlin
 * // build.gradle.kts
 *
 * dokka {
 *   dokkaSourceSets {
 *     // configure individual source set by name
 *     named("customSourceSet") {
 *       // ...
 *     }
 *
 *     // configure all source sets at once
 *     configureEach {
 *       // ...
 *     }
 *   }
 * }
 * ```
 *
 * @see org.jetbrains.dokka.DokkaSourceSetImpl
 */
abstract class DokkaSourceSetSpec
@InternalDokkaGradlePluginApi
@Inject
constructor(
    private val name: String,
    private val objects: ObjectFactory,
) :
    HasConfigurableVisibilityModifiers,
    Named,
    Serializable,
    ExtensionAware {

    @Internal // will be tracked by sourceSetId
    override fun getName(): String = name

    /**
     * An arbitrary string used to group source sets that originate from different Gradle subprojects.
     * This is primarily used by Kotlin Multiplatform projects, which can have multiple source sets
     * per subproject.
     *
     * The default is set from [DokkaExtension.sourceSetScopeDefault][org.jetbrains.dokka.gradle.DokkaExtension.sourceSetScopeDefault].
     *
     * It's unlikely that this value needs to be changed.
     */
    @get:Internal // will be tracked by sourceSetId
    abstract val sourceSetScope: Property<String>

    /**
     * The identifier for this source set, which is distinct across all Gradle subprojects.
     *
     * @see sourceSetScope
     * @see getName
     */
    @get:Input
    val sourceSetId: Provider<SourceSetIdSpec>
        get() = sourceSetScope.map { scope -> objects.dokkaSourceSetIdSpec(scope, getName()) }

    /**
     * Whether this source set should be skipped when generating documentation.
     *
     * Default is `false`.
     */
    @get:Input
    abstract val suppress: Property<Boolean>

    /**
     * Display name used to refer to the source set.
     *
     * The name will be used both externally (for example, source set name visible to documentation readers) and
     * internally (for example, for logging messages of [reportUndocumented]).
     *
     * By default, the value is deduced from information provided by the Kotlin Gradle plugin.
     */
    @get:Input
    abstract val displayName: Property<String>

    /**
     * List of Markdown files that contain
     * [module and package documentation](https://kotlinlang.org/docs/reference/dokka-module-and-package-docs.html).
     *
     * Contents of specified files will be parsed and embedded into documentation as module and package descriptions.
     *
     * Example of such a file:
     *
     * ```markdown
     * # Module kotlin-demo
     *
     * The module shows the Dokka usage.
     *
     * # Package org.jetbrains.kotlin.demo
     *
     * Contains assorted useful stuff.
     *
     * ## Level 2 heading
     *
     * Text after this heading is also part of documentation for `org.jetbrains.kotlin.demo`
     *
     * # Package org.jetbrains.kotlin.demo2
     *
     * Useful stuff in another package.
     * ```
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ConfigurableFileCollection

    /**
     * Set of visibility modifiers that should be documented.
     *
     * This can be used if you want to document protected/internal/private declarations,
     * as well as if you want to exclude public declarations and only document internal API.
     *
     * Can be configured on per-package basis, see [DokkaPackageOptionsSpec.documentedVisibilities].
     *
     * Default is [VisibilityModifier.Public].
     */
    @get:Input
    abstract override val documentedVisibilities: SetProperty<VisibilityModifier>

    /**
     * Specifies source sets that current source set depends on.
     *
     * Among other things, this information is needed to resolve
     * [expect/actual](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html) declarations.
     *
     * By default, the values are deduced from information provided by the Kotlin Gradle plugin.
     */
    @get:Nested
    val dependentSourceSets: NamedDomainObjectContainer<SourceSetIdSpec> =
        extensions.adding("dependentSourceSets", objects.domainObjectContainer())

    /**
     * Classpath for analysis and interactive samples.
     *
     * Useful if some types that come from dependencies are not resolved/picked up automatically.
     * Property accepts both `.jar` and `.klib` files.
     *
     * By default, classpath is deduced from information provided by the Kotlin Gradle plugin.
     */
    @get:Classpath
    @get:Optional
    abstract val classpath: ConfigurableFileCollection

    /**
     * Source code roots to be analyzed and documented.
     * Accepts directories and individual `.kt` / `.java` files.
     *
     * By default, source roots are deduced from information provided by the Kotlin Gradle plugin.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoots: ConfigurableFileCollection

    /**
     * List of directories or files that contain sample functions which are referenced via
     * [`@sample`](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier) KDoc tag.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val samples: ConfigurableFileCollection

    /**
     * Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
     * after they have been filtered by [documentedVisibilities].
     *
     * Can be overridden for a specific package by setting [DokkaPackageOptionsSpec.reportUndocumented].
     *
     * Default is `false`.
     */
    @get:Input
    abstract val reportUndocumented: Property<Boolean>

    /**
     * Specifies the location of the project source code on the Web. If provided, Dokka generates
     * "source" links for each declaration. See [DokkaSourceLinkSpec] for more details.
     *
     * Prefer using [sourceLink] action/closure for adding source links.
     *
     * @see sourceLink
     */
    @get:Nested
    abstract val sourceLinks: DomainObjectSet<DokkaSourceLinkSpec>

    /**
     * Allows customising documentation generation options on a per-package basis.
     *
     * Use the [perPackageOptions] function to add a new item.
     *
     * @see DokkaPackageOptionsSpec
     */
    @get:Nested
    abstract val perPackageOptions: DomainObjectSet<DokkaPackageOptionsSpec>

    /**
     * Allows linking to Dokka/Javadoc documentation of the project's dependencies.
     */
    @get:Nested
    val externalDocumentationLinks: NamedDomainObjectContainer<DokkaExternalDocumentationLinkSpec> =
        extensions.adding("externalDocumentationLinks", objects.domainObjectContainer())

    /**
     * Platform to be used for setting up code analysis and samples.
     *
     * The default value is deduced from information provided by the Kotlin Gradle plugin.
     */
    @get:Input
    abstract val analysisPlatform: Property<KotlinPlatform>

    /**
     * Whether to skip packages that contain no visible declarations after
     * various filters have been applied.
     *
     * For instance, if [skipDeprecated] is set to `true` and your package contains only
     * deprecated declarations, it will be considered to be empty.
     *
     * Default is `true`.
     */
    @get:Input
    abstract val skipEmptyPackages: Property<Boolean>

    /**
     * Whether to document declarations annotated with [Deprecated].
     *
     * Can be overridden on package level by setting [DokkaPackageOptionsSpec.skipDeprecated].
     *
     * Default is `false`.
     */
    @get:Input
    abstract val skipDeprecated: Property<Boolean>

    /**
     * Directories or individual files that should be suppressed, meaning declarations from them
     * will be not documented.
     *
     * Will be concatenated with generated files if [suppressGeneratedFiles] is set to `false`.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val suppressedFiles: ConfigurableFileCollection

    /**
     * Whether to document/analyze generated files.
     *
     * Generated files are expected to be present under `{project}/{buildDir}/generated` directory.
     * If set to `true`, it effectively adds all files from that directory to [suppressedFiles], so
     * you can configure it manually.
     *
     * Default is `true`.
     */
    @get:Input
    abstract val suppressGeneratedFiles: Property<Boolean>

    /**
     * Whether to generate external documentation links that lead to API reference documentation for
     * Kotlin's standard library when declarations from it are used.
     *
     * Default is `true`, meaning links will be generated.
     *
     * @see externalDocumentationLinks
     */
    @get:Input
    abstract val enableKotlinStdLibDocumentationLink: Property<Boolean>

    /**
     * Whether to generate external documentation links to JDK's Javadocs when declarations from it
     * are used.
     *
     * The version of JDK Javadocs is determined by [jdkVersion] property.
     *
     * Default is `true`, meaning links will be generated.
     *
     * @see externalDocumentationLinks
     */
    @get:Input
    abstract val enableJdkDocumentationLink: Property<Boolean>

    /**
     * Whether to generate external documentation links for Android SDK API reference when
     * declarations from it are used.
     *
     * Only relevant in Android projects, and will be automatically disabled otherwise.
     *
     * The default value is automatically determined.
     * If [analysisPlatform] is set to [KotlinPlatform.AndroidJVM], then the value will be `true`.
     * Otherwise, the value defaults to `false`.
     *
     * @see externalDocumentationLinks
     */
    @get:Input
    abstract val enableAndroidDocumentationLink: Property<Boolean>

    /**
     * [Kotlin language version](https://kotlinlang.org/docs/compatibility-modes.html)
     * used for setting up analysis and [`@sample`](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
     * environment.
     *
     * By default, the latest language version available to Dokka's embedded compiler will be used.
     */
    @get:Input
    @get:Optional
    abstract val languageVersion: Property<String?>

    /**
     * [Kotlin API version](https://kotlinlang.org/docs/compatibility-modes.html)
     * used for setting up analysis and [`@sample`](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
     * environment.
     *
     * By default, it will be deduced from [languageVersion].
     */
    @get:Input
    @get:Optional
    abstract val apiVersion: Property<String?>

    /**
     * JDK version to use when generating external documentation links for Java types.
     *
     * For instance, if you use [java.util.UUID] from JDK in some public declaration signature,
     * and this property is set to `8`, Dokka will generate an external documentation link
     * to [JDK 8 Javadocs](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html) for it.
     *
     * Default is JDK 11.
     */
    @get:Input
    abstract val jdkVersion: Property<Int>

    /**
     * Configure and add a new source link to [sourceLinks].
     *
     * @see DokkaSourceLinkSpec
     */
    fun sourceLink(action: Action<in DokkaSourceLinkSpec>) {
        sourceLinks.add(
            objects.newInstance(DokkaSourceLinkSpec::class).also {
                action.execute(it)
            }
        )
    }

    /**
     * Action for configuring package options, appending to [perPackageOptions].
     *
     * @see DokkaPackageOptionsSpec
     */
    fun perPackageOption(action: Action<in DokkaPackageOptionsSpec>) {
        perPackageOptions.add(
            objects.newInstance(DokkaPackageOptionsSpec::class).also {
                action.execute(it)
            }
        )
    }

    //region deprecated properties
    /**
     * This property is replaced with [enableKotlinStdLibDocumentationLink].
     *
     * **Important**: When replacing [noStdlibLink] you **must** invert the value.
     *
     * ```kotlin
     * // Before:
     * noStdlibLink.set(true)
     *
     * // Update the name, and set the value to `false` instead of `true`:
     * enableKotlinStdLibDocumentationLink.set(false)
     * ```
     *
     * @see enableKotlinStdLibDocumentationLink
     */
    @Deprecated("Replaced with to enableKotlinStdLibDocumentationLink. The value must be inverted.", level = ERROR)
    @get:Internal
    @Suppress("unused")
    abstract val noStdlibLink: Property<Boolean>

    /**
     * This property is replaced with [enableAndroidDocumentationLink].
     *
     * **Important**: When replacing [noAndroidSdkLink] you **must** invert the value.
     *
     * ```kotlin
     * // Before:
     * noAndroidSdkLink.set(true)
     *
     * // Update the name, and set the value to `false` instead of `true`:
     * enableAndroidDocumentationLink.set(false)
     * ```
     *
     * @see enableAndroidDocumentationLink
     */
    @Deprecated("Replaced with to enableAndroidDocumentationLink. The value must be inverted.", level = ERROR)
    @get:Internal
    @Suppress("unused")
    abstract val noAndroidSdkLink: Property<Boolean>

    /**
     * This property is replaced with [enableJdkDocumentationLink].
     *
     * **Important**: When replacing [noJdkLink] you **must** invert the value.
     *
     * ```kotlin
     * // Before:
     * noJdkLink.set(true)
     *
     * // Update the name, and set the value to `false` instead of `true`:
     * enableJdkDocumentationLink.set(false)
     * ```
     *
     * @see enableJdkDocumentationLink
     */
    @Deprecated("Replaced with to enableJdkDocumentationLink. The value must be inverted.", level = ERROR)
    @get:Internal
    @Suppress("unused")
    abstract val noJdkLink: Property<Boolean>
    //endregion

    companion object {
        internal fun ObjectFactory.dokkaSourceSetSpecContainer(): NamedDomainObjectContainer<DokkaSourceSetSpec> =
            domainObjectContainer { name ->
                newInstance<DokkaSourceSetSpec>(name).apply {
                    // Manually added sourceSets should not be suppressed by default.
                    // dokkaSourceSets that are automatically added by DokkaKotlinAdapter will have a sensible value for 'suppress'.
                    //suppress.convention(false)
                    //analysisPlatform.convention(KotlinPlatform.DEFAULT)
                }
            }
    }
}
