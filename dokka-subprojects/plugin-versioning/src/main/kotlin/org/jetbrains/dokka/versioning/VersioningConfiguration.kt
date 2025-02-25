/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.versioning

import org.jetbrains.dokka.plugability.ConfigurableBlock
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

public data class VersioningConfiguration(
    var olderVersionsDir: File? = defaultOlderVersionsDir,
    var olderVersions: List<File>? = defaultOlderVersions,
    var versionsOrdering: List<String>? = defaultVersionsOrdering,
    var version: String? = defaultVersion,
    var renderVersionsNavigationOnAllPages: Boolean? = defaultRenderVersionsNavigationOnAllPages,
    var olderVersionsDirName: String = defaultOlderVersionsDirName
) : ConfigurableBlock {
    internal fun versionFromConfigurationOrModule(dokkaContext: DokkaContext): String =
        version ?: dokkaContext.configuration.moduleVersion ?: "1.0"

    internal fun allOlderVersions(): List<File> {
        if (olderVersionsDir != null)
            assert(olderVersionsDir!!.isDirectory) { "Supplied previous version $olderVersionsDir is not a directory!" }

        return olderVersionsDir?.listFiles()
            ?.filter { it.isDirectory && !it.isHidden }
            ?.toList().orEmpty() + olderVersions.orEmpty()
    }

    public companion object {
        public val defaultOlderVersionsDir: File? = null
        public val defaultOlderVersions: List<File>? = null
        public val defaultVersionsOrdering: List<String>? = null
        public val defaultVersion: String? = null
        public val defaultRenderVersionsNavigationOnAllPages: Boolean = true
        public val defaultOlderVersionsDirName: String = "older"

        @Deprecated("Replaced with VersioningConfiguration.Companion.defaultOlderVersionsDirName and VersioningConfiguration.olderVersionsDirName")
        public const val OLDER_VERSIONS_DIR: String = "older"
        public const val VERSIONS_FILE: String = "version.json"
    }
}
