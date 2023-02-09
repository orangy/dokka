package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.dokka.DokkaConfigurationBuilder
import java.io.File
import java.io.Serializable
import java.net.URL

/**
 * Configuration builder that allows adding a `source` link to each signature
 * which leads to [remoteUrl] with a specific line number (configurable by setting [remoteLineSuffix]),
 * letting documentation readers find source code for each declaration.
 *
 * Example in Gradle Kotlin DSL:
 *
 * ```kotlin
 * sourceLink {
 *     localDirectory.set(projectDir.resolve("src"))
 *     remoteUrl.set(URL("https://github.com/kotlin/dokka/tree/master/src"))
 *     remoteLineSuffix.set("#L")
 * }
 * ```
 */
abstract class DokkaSourceLinkGradleBuilder :
    DokkaConfigurationBuilder<DokkaConfigurationKxs.SourceLinkDefinitionKxs>,
    Serializable {

    /**
     * Path to the local source directory. The path must be relative to the root of current project.
     *
     * This path is used to find relative paths of the source files from which the documentation is built.
     * These relative paths are then combined with the base url of a source code hosting service specified with
     * the [remoteUrl] property to create source links for each declaration.
     *
     * Example:
     *
     * ```kotlin
     * projectDir.resolve("src")
     * ```
     */
    @get:Internal // changing contents of the directory should not invalidate the task
    abstract val localDirectory: Property<File>

    /**
     * The relative path to [localDirectory] from the project directory. Declared as an input to invalidate the task if that path changes.
     * Should not be used anywhere directly.
     */
//    @Suppress("unused")
    @get:Input
    protected val localDirectoryPath: Provider<String>
        get() = localDirectory.map { it.path }

    /**
     * URL of source code hosting service that can be accessed by documentation readers,
     * like GitHub, GitLab, Bitbucket, etc. This URL will be used to generate
     * source code links of declarations.
     *
     * Example:
     *
     * ```kotlin
     * java.net.URL("https://github.com/username/projectname/tree/master/src"))
     * ```
     */
    @get:Input
    abstract val remoteUrl: Property<URL>

    /**
     * Suffix used to append source code line number to the URL. This will help readers navigate
     * not only to the file, but to the specific line number of the declaration.
     *
     * The number itself will be appended to the specified suffix. For instance,
     * if this property is set to `#L` and the line number is 10, resulting URL suffix
     * will be `#L10`
     *
     * Suffixes used by popular services:
     * - GitHub: `#L`
     * - GitLab: `#L`
     * - Bitbucket: `#lines-`
     *
     * Default is `#L`.
     */
    @get:Optional
    @get:Input
    abstract val remoteLineSuffix: Property<String>

    override fun build(): DokkaConfigurationKxs.SourceLinkDefinitionKxs {
        return DokkaConfigurationKxs.SourceLinkDefinitionKxs(
            localDirectory = localDirectory.get().canonicalPath,
            remoteUrl = remoteUrl.get(),
            remoteLineSuffix = remoteLineSuffix.orNull,
        )
    }
}
