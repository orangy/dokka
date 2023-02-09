package org.jetbrains.dokka.gradle

import org.gradle.api.provider.Provider

internal fun Provider<String>.getValidVersionOrNull() = orNull?.takeIf { it != "unspecified" }
