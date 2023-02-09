package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.DokkaConfigurationBuilder
import java.io.Serializable
import java.net.URL

/**
 * Configuration builder that allows creating links leading to externally hosted
 * documentation of your dependencies.
 *
 * For instance, if you are using types from `kotlinx.serialization`, by default
 * they will be unclickable in your documentation, as if unresolved. However,
 * since API reference for `kotlinx.serialization` is also built by Dokka and is
 * [published on kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/),
 * you can configure external documentation links for it, allowing Dokka to generate
 * documentation links for used types, making them clickable and appear resolved.
 *
 * Example in Gradle Kotlin DSL:
 *
 * ```kotlin
 * externalDocumentationLink {
 *     url.set(URL("https://kotlinlang.org/api/kotlinx.serialization/"))
 *     packageListUrl.set(
 *         rootProject.projectDir.resolve("serialization.package.list").toURL()
 *     )
 * }
 * ```
 */
abstract class DokkaExternalDocumentationLinkGradleBuilder :
    DokkaConfigurationBuilder<DokkaConfigurationKxs.ExternalDocumentationLinkKxs>,
    Serializable {

    /**
     * Root URL of documentation to link with. **Must** contain a trailing slash.
     *
     * Dokka will do its best to automatically find `package-list` for the given URL, and link
     * declarations together.
     *
     * It automatic resolution fails or if you want to use locally cached files instead,
     * consider providing [packageListUrl].
     *
     * Example:
     *
     * ```kotlin
     * java.net.URL("https://kotlinlang.org/api/kotlinx.serialization/")
     * ```
     */
    @get:Input
    abstract val url: Property<URL>

    /**
     * Specifies the exact location of a `package-list` instead of relying on Dokka
     * automatically resolving it. Can also be a locally cached file to avoid network calls.
     *
     * Example:
     *
     * ```kotlin
     * rootProject.projectDir.resolve("serialization.package.list").toURL()
     * ```
     */
    @get:Input
    abstract val packageListUrl: Property<URL>

    override fun build() =
        DokkaConfigurationKxs.ExternalDocumentationLinkKxs(
            url = url.get(),
            packageListUrl = packageListUrl.get(),
        )
}
