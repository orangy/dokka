package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.dokka.*
import java.io.File
import java.io.Serializable
import java.net.URL
import javax.inject.Inject

/**
 * [Source set](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets) level configuration.
 *
 * Can be configured in the following way with Gradle Kotlin DSL:
 *
 * ```kotlin
 * import org.jetbrains.dokka.gradle.DokkaTask
 *
 * tasks.dokkaHtml {
 *     dokkaSourceSets {
 *         // configure individual source set by name
 *         named("customSourceSet") {
 *             suppress.set(true)
 *         }
 *
 *         // configure all source sets at once
 *         configureEach {
 *             reportUndocumented.set(true)
 *         }
 *     }
 * }
 * ```
 */
abstract class DokkaSourceSetGradleBuilder(
    private val name: String
) :
    DokkaConfigurationBuilder<DokkaConfigurationKxs.DokkaSourceSetKxs>,
    Named,
    Serializable {

    @Internal
    override fun getName(): String = name

    @get:Inject
    protected abstract val objects: ObjectFactory

    @get:Inject
    protected abstract val layout: ProjectLayout

    @get:Input
    val sourceSetID: Provider<DokkaSourceSetID>
        get() = sourceSetScope.map { scope -> DokkaSourceSetID(scope, getName()) }

    @get:Input
    abstract val sourceSetScope: Property<String>

    // Name of KotlinSourceSet to automagically configure later
    // Adapter for the old DSL, because the old DSL had task-based configuration that I want to remove.
    // This property should be used to lazily set conventions for the source set properties.
    @get:Internal
    @Deprecated("hack for adapting the old DSL - replacement TBD")
    internal abstract val todoSourceSetName: Property<String>

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
     * Can be configured on per-package basis, see [DokkaPackageOptionsGradleBuilder.documentedVisibilities].
     *
     * Default is [DokkaConfiguration.Visibility.PUBLIC].
     */
    @get:Input
    abstract val documentedVisibilities: SetProperty<DokkaConfiguration.Visibility>

    /**
     * Specifies source sets that current source set depends on.
     *
     * Among other things, this information is needed to resolve
     * [expect/actual](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html) declarations.
     *
     * Prefer using [dependsOn] function to append dependent source sets to this list.
     *
     * By default, the values are deduced from information provided by the Kotlin Gradle plugin.
     */
    @get:Nested
    abstract val dependentSourceSets: NamedDomainObjectContainer<DokkaSourceSetIDGradleBuilder>

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
     * Prefer using [sourceRoot] function to append source roots to this list.
     *
     * By default, source roots are deduced from information provided by the Kotlin Gradle plugin.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoots: ConfigurableFileCollection

    /**
     * List of directories or files that contain sample functions which are referenced via
     * [@sample](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier) KDoc tag.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val samples: ConfigurableFileCollection

    /**
     * Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
     * after they have been filtered by [documentedVisibilities].
     *
     * This setting works well with [AbstractDokkaTask.failOnWarning].
     *
     * Can be overridden for a specific package by setting [DokkaPackageOptionsGradleBuilder.reportUndocumented].
     *
     * Default is `false`.
     */
    @get:Input
    abstract val reportUndocumented: Property<Boolean>

    /**
     * Specifies the location of the project source code on the Web. If provided, Dokka generates
     * "source" links for each declaration. See [DokkaSourceLinkGradleBuilder] for more details.
     *
     * Prefer using [sourceLink] action/closure for adding source links.
     *
     * @see sourceLink
     */
    @get:Nested
    abstract val sourceLinks: DomainObjectSet<DokkaSourceLinkGradleBuilder>

    /**
     * Allows to customize documentation generation options on a per-package basis.
     *
     * @see DokkaPackageOptionsGradleBuilder for details
     */
    @get:Nested
    abstract val perPackageOptions: DomainObjectSet<DokkaPackageOptionsGradleBuilder>

    /**
     * Allows linking to Dokka/Javadoc documentation of the project's dependencies.
     *
     * Prefer using [externalDocumentationLink] action/closure for adding links.
     */
    @get:Nested
    abstract val externalDocumentationLinks: DomainObjectSet<DokkaExternalDocumentationLinkGradleBuilder>

    /**
     * Platform to be used for setting up code analysis and samples.
     *
     * The default value is deduced from information provided by the Kotlin Gradle plugin.
     */
    @get:Input
    abstract val analysisPlatform: Property<Platform>

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
     * Can be overridden on package level by setting [DokkaPackageOptionsGradleBuilder.skipDeprecated].
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
     * Whether to generate external documentation links that lead to API reference
     * documentation for Kotlin's standard library when declarations from it are used.
     *
     * Default is `false`, meaning links will be generated.
     */
    @get:Input
    abstract val noStdlibLink: Property<Boolean>

    /**
     * Whether to generate external documentation links to JDK's Javadocs
     * when declarations from it are used.
     *
     * The version of JDK Javadocs is determined by [jdkVersion] property.
     *
     * Default is `false`, meaning links will be generated.
     */
    @get:Input
    abstract val noJdkLink: Property<Boolean>

    /**
     * Whether to generate external documentation links for Android SDK API reference
     * when declarations from it are used.
     *
     * Only relevant in Android projects, ignored otherwise.
     *
     * Default is `false`, meaning links will be generated.
     */
    @get:Input
    abstract val noAndroidSdkLink: Property<Boolean>

    /**
     * [Kotlin language version](https://kotlinlang.org/docs/compatibility-modes.html)
     * used for setting up analysis and [@sample](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
     * environment.
     *
     * By default, the latest language version available to Dokka's embedded compiler will be used.
     */
    @get:Input
    @get:Optional
    abstract val languageVersion: Property<String?>

    /**
     * [Kotlin API version](https://kotlinlang.org/docs/compatibility-modes.html)
     * used for setting up analysis and [@sample](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
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
     * Default is JDK 8.
     */
    @get:Input
    abstract val jdkVersion: Property<Int>

    fun DokkaSourceSetID(sourceSetName: String): DokkaSourceSetIDGradleBuilder {
        return dependentSourceSets.create("TODO figure out scope ID") {
            this.sourceSetName = sourceSetName
        }
    }

    /** Convenient override to **append** source sets to [dependentSourceSets] */
    fun dependsOn(sourceSet: SourceSet): Unit = dependsOn(sourceSet.name)

//    /** Convenient override to **append** source sets to [dependentSourceSets] */
//    fun dependsOn(sourceSet: DokkaSourceSetGradleBuilder): Unit {
//        dependentSourceSets.add(sourceSet.sourceSetID.get())
//    }

    /** Convenient override to **append** source sets to [dependentSourceSets] */
    fun dependsOn(sourceSet: DokkaConfiguration.DokkaSourceSet): Unit = dependsOn(sourceSet.sourceSetID)

    /** Convenient override to **append** source sets to [dependentSourceSets] */
    fun dependsOn(sourceSetName: String): Unit = dependsOn(sourceSetName)

    /** Convenient override to **append** source sets to [dependentSourceSets] */
    fun dependsOn(sourceSetID: DokkaSourceSetID) {
        dependentSourceSets.create(sourceSetID.scopeId) {
            sourceSetName = sourceSetID.sourceSetName
        }
    }

    /** Convenient override to **append** source roots to [sourceRoots] */
    fun sourceRoot(file: File) {
        sourceRoots.from(file)
    }

    /** Convenient override to **append** source roots to [sourceRoots] */
    fun sourceRoot(path: String) {
        sourceRoots.from(path)
    }

    /**
     * Action for configuring source links, appending to [sourceLinks].
     *
     * @see [DokkaSourceLinkGradleBuilder] for details.
     */
    fun sourceLink(action: Action<in DokkaSourceLinkGradleBuilder>) {
        sourceLinks.add(
            objects.newInstance(DokkaSourceLinkGradleBuilder::class).also {
                action.execute(it)
            }
        )
    }

//    /**
//     * Closure for configuring package options, appending to [perPackageOptions].
//     *
//     * @see [DokkaPackageOptionsGradleBuilder] for details.
//     */
//    @Suppress("DEPRECATION") // TODO [beresnev] ConfigureUtil will be removed in Gradle 8
//    fun perPackageOption(c: Closure<in DokkaPackageOptionsGradleBuilder>) {
//        val configured = org.gradle.util.ConfigureUtil.configure(c, DokkaPackageOptionsGradleBuilder())
//        perPackageOptions.add(configured)
//    }

    /**
     * Action for configuring package options, appending to [perPackageOptions].
     *
     * @see [DokkaPackageOptionsGradleBuilder] for details.
     */
    fun perPackageOption(action: Action<in DokkaPackageOptionsGradleBuilder>) {
        perPackageOptions.add(
            objects.newInstance(DokkaPackageOptionsGradleBuilder::class).also {
                action.execute(it)
            }
        )
    }

//    /**
//     * Closure for configuring external documentation links, appending to [externalDocumentationLinks].
//     *
//     * @see [GradleExternalDocumentationLinkBuilder] for details.
//     */
//    @Suppress("DEPRECATION") // TODO [beresnev] ConfigureUtil will be removed in Gradle 8
//    fun externalDocumentationLink(c: Closure<in GradleExternalDocumentationLinkBuilder>) {
//         val link = org.gradle.util.ConfigureUtil.configure(c, GradleExternalDocumentationLinkBuilder(project))
//        externalDocumentationLinks.add(link)
//    }

    /**
     * Action for configuring external documentation links, appending to [externalDocumentationLinks].
     *
     * See [DokkaExternalDocumentationLinkGradleBuilder] for details.
     */
    fun externalDocumentationLink(action: Action<in DokkaExternalDocumentationLinkGradleBuilder>) {
        externalDocumentationLinks.add(
            objects.newInstance(DokkaExternalDocumentationLinkGradleBuilder::class).also {
                action.execute(it)
            }
        )
    }

    /** Convenient override to **append** external documentation links to [externalDocumentationLinks]. */
    fun externalDocumentationLink(url: String, packageListUrl: String? = null) {
        externalDocumentationLink(URL(url), packageListUrl = packageListUrl?.let(::URL))
    }

    /** Convenient override to **append** external documentation links to [externalDocumentationLinks]. */
    fun externalDocumentationLink(url: URL, packageListUrl: URL? = null) {
        externalDocumentationLinks.add(
            objects.newInstance(DokkaExternalDocumentationLinkGradleBuilder::class).also {
                it.url.set(url)
                if (packageListUrl != null) {
                    it.packageListUrl.set(packageListUrl)
                }
            }
        )
    }

    override fun build(): DokkaConfigurationKxs.DokkaSourceSetKxs {
        return DokkaConfigurationKxs.DokkaSourceSetKxs(
            sourceSetID = sourceSetID.get(),
            displayName = displayName.get(),
            classpath = classpath.files.toList(),
            sourceRoots = sourceRoots.files,
            dependentSourceSets = dependentSourceSets.map(DokkaSourceSetIDGradleBuilder::build).toSet(),
            samples = samples.files,
            includes = includes.files,
            reportUndocumented = reportUndocumented.get(),
            skipEmptyPackages = skipEmptyPackages.get(),
            skipDeprecated = skipDeprecated.get(),
            jdkVersion = jdkVersion.get(),
            sourceLinks = sourceLinks.map(DokkaSourceLinkGradleBuilder::build).toSet(),
            perPackageOptions = perPackageOptions.map(DokkaPackageOptionsGradleBuilder::build),
            externalDocumentationLinks =
            externalDocumentationLinks.map(DokkaExternalDocumentationLinkGradleBuilder::build).toSet(),
            languageVersion = languageVersion.orNull,
            apiVersion = apiVersion.orNull,
            noStdlibLink = noStdlibLink.get(),
            noJdkLink = noJdkLink.get(),
            suppressedFiles = suppressedFiles.files,
            analysisPlatform = analysisPlatform.get(),
            documentedVisibilities = documentedVisibilities.get(),
        )
    }
}
