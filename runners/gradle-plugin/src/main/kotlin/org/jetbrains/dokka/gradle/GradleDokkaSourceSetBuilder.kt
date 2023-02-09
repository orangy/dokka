package org.jetbrains.dokka.gradle

import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.jetbrains.dokka.*
import java.io.File
import java.net.URL

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
open class GradleDokkaSourceSetBuilder(
    @Transient @get:Input val name: String,
    @Transient @get:Internal internal val project: Project,
    @Transient @get:Internal internal val sourceSetIdFactory: NamedDomainObjectFactory<DokkaSourceSetID>,
) : DokkaConfigurationBuilder<DokkaSourceSetImpl> {

    @Input
    val sourceSetID: DokkaSourceSetID = sourceSetIdFactory.create(name)

    /**
     * Whether this source set should be skipped when generating documentation.
     *
     * Default is `false`.
     */
    @Input
    val suppress: Property<Boolean> = project.objects.property<Boolean>()
        .convention(false)

    /**
     * Display name used to refer to the source set.
     *
     * The name will be used both externally (for example, source set name visible to documentation readers) and
     * internally (for example, for logging messages of [reportUndocumented]).
     *
     * By default, the value is deduced from information provided by the Kotlin Gradle plugin.
     */
    @Input
    val displayName: Property<String> = project.objects.property()

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
    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    val includes: ConfigurableFileCollection = project.files()

    /**
     * Set of visibility modifiers that should be documented.
     *
     * This can be used if you want to document protected/internal/private declarations,
     * as well as if you want to exclude public declarations and only document internal API.
     *
     * Can be configured on per-package basis, see [GradlePackageOptionsBuilder.documentedVisibilities].
     *
     * Default is [DokkaConfiguration.Visibility.PUBLIC].
     */
    @Input
    val documentedVisibilities: SetProperty<DokkaConfiguration.Visibility> = project.objects.setProperty<DokkaConfiguration.Visibility>().convention(DokkaDefaults.documentedVisibilities)

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
    @Input
    val dependentSourceSets: SetProperty<DokkaSourceSetID> = project.objects.setProperty<DokkaSourceSetID>().convention(emptySet())

    /**
     * Classpath for analysis and interactive samples.
     *
     * Useful if some types that come from dependencies are not resolved/picked up automatically.
     * Property accepts both `.jar` and `.klib` files.
     *
     * By default, classpath is deduced from information provided by the Kotlin Gradle plugin.
     */
    @Classpath
    @Optional
    val classpath: ConfigurableFileCollection = project.files()

    /**
     * Source code roots to be analyzed and documented.
     * Accepts directories and individual `.kt` / `.java` files.
     *
     * Prefer using [sourceRoot] function to append source roots to this list.
     *
     * By default, source roots are deduced from information provided by the Kotlin Gradle plugin.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val sourceRoots: ConfigurableFileCollection = project.objects.fileCollection()

    /**
     * List of directories or files that contain sample functions which are referenced via
     * [@sample](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier) KDoc tag.
     */
    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    val samples: ConfigurableFileCollection = project.files()

    /**
     * Whether to emit warnings about visible undocumented declarations, that is declarations without KDocs
     * after they have been filtered by [documentedVisibilities].
     *
     * This setting works well with [AbstractDokkaTask.failOnWarning].
     *
     * Can be overridden for a specific package by setting [GradlePackageOptionsBuilder.reportUndocumented].
     *
     * Default is `false`.
     */
    @Input
    val reportUndocumented: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.reportUndocumented)

    /**
     * Specifies the location of the project source code on the Web. If provided, Dokka generates
     * "source" links for each declaration. See [GradleSourceLinkBuilder] for more details.
     *
     * Prefer using [sourceLink] action/closure for adding source links.
     */
    @Nested
    val sourceLinks: SetProperty<GradleSourceLinkBuilder> = project.objects.setProperty<GradleSourceLinkBuilder>().convention(emptySet())

    /**
     * Allows to customize documentation generation options on a per-package basis.
     *
     * @see GradlePackageOptionsBuilder for details
     */
    @Nested
    val perPackageOptions: ListProperty<GradlePackageOptionsBuilder> =
        project.objects.listProperty<GradlePackageOptionsBuilder>()
            .convention(emptyList())

    /**
     * Allows linking to Dokka/Javadoc documentation of the project's dependencies.
     *
     * Prefer using [externalDocumentationLink] action/closure for adding links.
     */
    @Nested
    val externalDocumentationLinks: SetProperty<GradleExternalDocumentationLinkBuilder> =
        project.objects.setProperty<GradleExternalDocumentationLinkBuilder>()
            .convention(emptySet())

    /**
     * Platform to be used for setting up code analysis and samples.
     *
     * The default value is deduced from information provided by the Kotlin Gradle plugin.
     */
    @Input
    @Optional
    val platform: Property<Platform> = project.objects.property<Platform>()
        .convention(Platform.DEFAULT)

    /**
     * Whether to skip packages that contain no visible declarations after
     * various filters have been applied.
     *
     * For instance, if [skipDeprecated] is set to `true` and your package contains only
     * deprecated declarations, it will be considered to be empty.
     *
     * Default is `true`.
     */
    @Input
    val skipEmptyPackages: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.skipEmptyPackages)

    /**
     * Whether to document declarations annotated with [Deprecated].
     *
     * Can be overridden on package level by setting [GradlePackageOptionsBuilder.skipDeprecated].
     *
     * Default is `false`.
     */
    @Input
    val skipDeprecated: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.skipDeprecated)

    /**
     * Directories or individual files that should be suppressed, meaning declarations from them
     * will be not documented.
     *
     * Will be concatenated with generated files if [suppressGeneratedFiles] is set to `false`.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val suppressedFiles: ConfigurableFileCollection = project.files()

    /**
     * Whether to document/analyze generated files.
     *
     * Generated files are expected to be present under `{project}/{buildDir}/generated` directory.
     * If set to `true`, it effectively adds all files from that directory to [suppressedFiles], so
     * you can configure it manually.
     *
     * Default is `true`.
     */
    @Input
    val suppressGeneratedFiles: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.suppressGeneratedFiles)

    /**
     * Whether to generate external documentation links that lead to API reference
     * documentation for Kotlin's standard library when declarations from it are used.
     *
     * Default is `false`, meaning links will be generated.
     */
    @Input
    val noStdlibLink: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.noStdlibLink)

    /**
     * Whether to generate external documentation links to JDK's Javadocs
     * when declarations from it are used.
     *
     * The version of JDK Javadocs is determined by [jdkVersion] property.
     *
     * Default is `false`, meaning links will be generated.
     */
    @Input
    val noJdkLink: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.noJdkLink)

    /**
     * Whether to generate external documentation links for Android SDK API reference
     * when declarations from it are used.
     *
     * Only relevant in Android projects, ignored otherwise.
     *
     * Default is `false`, meaning links will be generated.
     */
    @Input
    val noAndroidSdkLink: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.noAndroidSdkLink)

    /**
     * [Kotlin language version](https://kotlinlang.org/docs/compatibility-modes.html)
     * used for setting up analysis and [@sample](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
     * environment.
     *
     * By default, the latest language version available to Dokka's embedded compiler will be used.
     */
    @Input
    @Optional
    val languageVersion: Property<String?> = project.objects.property()

    /**
     * [Kotlin API version](https://kotlinlang.org/docs/compatibility-modes.html)
     * used for setting up analysis and [@sample](https://kotlinlang.org/docs/kotlin-doc.html#sample-identifier)
     * environment.
     *
     * By default, it will be deduced from [languageVersion].
     */
    @Input
    @Optional
    val apiVersion: Property<String?> = project.objects.property()

    /**
     * JDK version to use when generating external documentation links for Java types.
     *
     * For instance, if you use [java.util.UUID] from JDK in some public declaration signature,
     * and this property is set to `8`, Dokka will generate an external documentation link
     * to [JDK 8 Javadocs](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html) for it.
     *
     * Default is JDK 8.
     */
    @Input
    val jdkVersion: Property<Int> = project.objects.property<Int>()
        .convention(DokkaDefaults.jdkVersion)

    /**
     * Deprecated. Use [documentedVisibilities] instead.
     */
    @Input
    val includeNonPublic: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.includeNonPublic)

    fun DokkaSourceSetID(sourceSetName: String): DokkaSourceSetID = sourceSetIdFactory.create(sourceSetName)

    /**
     * Convenient override to **append** source sets to [dependentSourceSets]
     */
    fun dependsOn(sourceSet: SourceSet) {
        dependsOn(DokkaSourceSetID(sourceSet.name))
    }

    /**
     * Convenient override to **append** source sets to [dependentSourceSets]
     */
    fun dependsOn(sourceSet: GradleDokkaSourceSetBuilder) {
        dependsOn(sourceSet.sourceSetID)
    }

    /**
     * Convenient override to **append** source sets to [dependentSourceSets]
     */
    fun dependsOn(sourceSet: DokkaConfiguration.DokkaSourceSet) {
        dependsOn(sourceSet.sourceSetID)
    }

    /**
     * Convenient override to **append** source sets to [dependentSourceSets]
     */
    fun dependsOn(sourceSetName: String) {
        dependsOn(DokkaSourceSetID(sourceSetName))
    }

    /**
     * Convenient override to **append** source sets to [dependentSourceSets]
     */
    fun dependsOn(sourceSetID: DokkaSourceSetID) {
        dependentSourceSets.add(sourceSetID)
    }

    /**
     * Convenient override to **append** source roots to [sourceRoots]
     */
    fun sourceRoot(file: File) {
        sourceRoots.from(file)
    }

    /**
     * Convenient override to **append** source roots to [sourceRoots]
     */
    fun sourceRoot(path: String) {
        sourceRoot(project.file(path))
    }

    /**
     * Closure for configuring source links, appending to [sourceLinks].
     *
     * @see [GradleSourceLinkBuilder] for details.
     */
    @Suppress("DEPRECATION") // TODO [beresnev] ConfigureUtil will be removed in Gradle 8
    fun sourceLink(c: Closure<in GradleSourceLinkBuilder>) {
        val configured = org.gradle.util.ConfigureUtil.configure(c, GradleSourceLinkBuilder(project))
        sourceLinks.add(configured)
    }

    /**
     * Action for configuring source links, appending to [sourceLinks].
     *
     * @see [GradleSourceLinkBuilder] for details.
     */
    fun sourceLink(action: Action<in GradleSourceLinkBuilder>) {
        val sourceLink = GradleSourceLinkBuilder(project)
        action.execute(sourceLink)
        sourceLinks.add(sourceLink)
    }

    /**
     * Closure for configuring package options, appending to [perPackageOptions].
     *
     * @see [GradlePackageOptionsBuilder] for details.
     */
    @Suppress("DEPRECATION") // TODO [beresnev] ConfigureUtil will be removed in Gradle 8
    fun perPackageOption(c: Closure<in GradlePackageOptionsBuilder>) {
        val configured = org.gradle.util.ConfigureUtil.configure(c, GradlePackageOptionsBuilder(project))
        perPackageOptions.add(configured)
    }

    /**
     * Action for configuring package options, appending to [perPackageOptions].
     *
     * @see [GradlePackageOptionsBuilder] for details.
     */
    fun perPackageOption(action: Action<in GradlePackageOptionsBuilder>) {
        val option = GradlePackageOptionsBuilder(project)
        action.execute(option)
        perPackageOptions.add(option)
    }

    /**
     * Closure for configuring external documentation links, appending to [externalDocumentationLinks].
     *
     * @see [GradleExternalDocumentationLinkBuilder] for details.
     */
    @Suppress("DEPRECATION") // TODO [beresnev] ConfigureUtil will be removed in Gradle 8
    fun externalDocumentationLink(c: Closure<in GradleExternalDocumentationLinkBuilder>) {
        val link = org.gradle.util.ConfigureUtil.configure(c, GradleExternalDocumentationLinkBuilder(project))
        externalDocumentationLinks.add(link)
    }

    /**
     * Action for configuring external documentation links, appending to [externalDocumentationLinks].
     *
     * See [GradleExternalDocumentationLinkBuilder] for details.
     */
    fun externalDocumentationLink(action: Action<in GradleExternalDocumentationLinkBuilder>) {
        val link = GradleExternalDocumentationLinkBuilder(project)
        action.execute(link)
        externalDocumentationLinks.add(link)
    }

    /**
     * Convenient override to **append** external documentation links to [externalDocumentationLinks].
     */
    fun externalDocumentationLink(url: String, packageListUrl: String? = null) {
        externalDocumentationLink(URL(url), packageListUrl = packageListUrl?.let(::URL))
    }

    /**
     * Convenient override to **append** external documentation links to [externalDocumentationLinks].
     */
    fun externalDocumentationLink(url: URL, packageListUrl: URL? = null) {
        externalDocumentationLinks.add(
            GradleExternalDocumentationLinkBuilder(project).apply {
                this.url.convention(url)
                if (packageListUrl != null) {
                    this.packageListUrl.convention(packageListUrl)
                }
            }
        )
    }

    override fun build(): DokkaSourceSetImpl = toDokkaSourceSetImpl()
}
