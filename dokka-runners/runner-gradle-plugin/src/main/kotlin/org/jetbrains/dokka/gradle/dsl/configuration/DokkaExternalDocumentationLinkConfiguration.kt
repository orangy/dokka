/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.configuration

import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl
import java.net.URI

// TODO: packageList should be downloaded via Gradle and not inside Dokka
@DokkaGradlePluginDsl
public interface DokkaExternalDocumentationLinkConfiguration {
    public val url: Property<String>
    public val packageListLocation: Property<URI>

    public fun packageListUrl(url: String)
    public fun packageListFile(path: Any)
}