@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.DokkaDefaults
import org.jetbrains.dokka.PackageOptionsImpl

/**
 * Configuration builder that allows setting some options for specific packages
 * matched by [matchingRegex].
 *
 * Example in Gradle Kotlin DSL:
 *
 * ```kotlin
 * tasks.dokkaHtml {
 *     dokkaSourceSets.configureEach {
 *         perPackageOption {
 *             matchingRegex.set(".*internal.*")
 *             suppress.set(true)
 *         }
 *     }
 * }
 * ```
 */
class GradlePackageOptionsBuilder(
    @Transient @get:Internal internal val project: Project
) : DokkaConfigurationBuilder<PackageOptionsImpl> {

    /**
     * Regular expression that is used to match the package.
     *
     * Default is any string: `.*`.
     */
    @Input
    val matchingRegex: Property<String> = project.objects.property<String>()
        .convention(".*")

    /**
     * Whether this package should be skipped when generating documentation.
     *
     * Default is `false`.
     */
    @Input
    val suppress: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.suppress)

    /**
     * Set of visibility modifiers that should be documented.
     *
     * This can be used if you want to document protected/internal/private declarations within a
     * specific package, as well as if you want to exclude public declarations and only document internal API.
     *
     * Can be configured for a whole source set, see [GradleDokkaSourceSetBuilder.documentedVisibilities].
     *
     * Default is [DokkaConfiguration.Visibility.PUBLIC].
     */
    @Input
    val documentedVisibilities: SetProperty<DokkaConfiguration.Visibility> = project.objects.setProperty<DokkaConfiguration.Visibility>()
        .convention(DokkaDefaults.documentedVisibilities)

    /**
     * Whether to document declarations annotated with [Deprecated].
     *
     * Can be overridden on source set level by setting [GradleDokkaSourceSetBuilder.skipDeprecated].
     *
     * Default is `false`.
     */
    @Input
    val skipDeprecated: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.skipDeprecated)

    /**
     * Whether to emit warnings about visible undocumented declarations, that is declarations from
     * this package and without KDocs, after they have been filtered by [documentedVisibilities].
     *
     * This setting works well with [AbstractDokkaTask.failOnWarning].
     *
     * Can be overridden on source set level by setting [GradleDokkaSourceSetBuilder.reportUndocumented].
     *
     * Default is `false`.
     */
    @Input
    val reportUndocumented: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.reportUndocumented)

    /**
     * Deprecated. Use [documentedVisibilities] instead.
     */
    @Input
    val includeNonPublic: Property<Boolean> = project.objects.property<Boolean>()
        .convention(DokkaDefaults.includeNonPublic)


    override fun build(): PackageOptionsImpl = PackageOptionsImpl(
        matchingRegex = checkNotNull(matchingRegex.get()) { "prefix not specified" },
        includeNonPublic = includeNonPublic.get(),
        documentedVisibilities = documentedVisibilities.get(),
        reportUndocumented = reportUndocumented.get(),
        skipDeprecated = skipDeprecated.get(),
        suppress = suppress.get()
    )
}
