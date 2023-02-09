@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationBuilder
import java.io.Serializable

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
abstract class DokkaPackageOptionsGradleBuilder :
    DokkaConfigurationBuilder<DokkaConfigurationKxs.PackageOptionsKxs>,
    Serializable {

    /**
     * Regular expression that is used to match the package.
     *
     * Default is any string: `.*`.
     */
    @get:Input
    abstract val matchingRegex: Property<String>

    /**
     * Whether this package should be skipped when generating documentation.
     *
     * Default is `false`.
     */
    @get:Input
    abstract val suppress: Property<Boolean>

    /**
     * Set of visibility modifiers that should be documented.
     *
     * This can be used if you want to document protected/internal/private declarations within a
     * specific package, as well as if you want to exclude public declarations and only document internal API.
     *
     * Can be configured for a whole source set, see [DokkaSourceSetGradleBuilder.documentedVisibilities].
     *
     * Default is [DokkaConfiguration.Visibility.PUBLIC].
     */
    @get:Input
    abstract val documentedVisibilities: SetProperty<DokkaConfiguration.Visibility>

    /**
     * Whether to document declarations annotated with [Deprecated].
     *
     * Can be overridden on source set level by setting [DokkaSourceSetGradleBuilder.skipDeprecated].
     *
     * Default is `false`.
     */
    @get:Input
    abstract val skipDeprecated: Property<Boolean>

    /**
     * Whether to emit warnings about visible undocumented declarations, that is declarations from
     * this package and without KDocs, after they have been filtered by [documentedVisibilities].
     *
     * This setting works well with [AbstractDokkaTask.failOnWarning].
     *
     * Can be overridden on source set level by setting [DokkaSourceSetGradleBuilder.reportUndocumented].
     *
     * Default is `false`.
     */
    @get:Input
    abstract val reportUndocumented: Property<Boolean>

    /**
     * Deprecated. Use [documentedVisibilities] instead.
     */
    @get:Input
    @Deprecated("Use documentedVisibilities instead")
    abstract val includeNonPublic: Property<Boolean>


    override fun build() = DokkaConfigurationKxs.PackageOptionsKxs(
        matchingRegex = matchingRegex.get(),
        documentedVisibilities = documentedVisibilities.get(),
        reportUndocumented = reportUndocumented.get(),
        skipDeprecated = skipDeprecated.get(),
        suppress = suppress.get()
    )
}
