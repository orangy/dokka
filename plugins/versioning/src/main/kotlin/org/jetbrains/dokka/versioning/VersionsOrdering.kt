/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.versioning

import org.apache.maven.artifact.versioning.ComparableVersion
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration

fun interface VersionsOrdering {
    fun order(records: List<VersionId>): List<VersionId>
}

class ByConfigurationVersionOrdering(val dokkaContext: DokkaContext) : VersionsOrdering {
    override fun order(records: List<VersionId>): List<VersionId> =
        configuration<VersioningPlugin, VersioningConfiguration>(dokkaContext)?.versionsOrdering
            ?: throw IllegalStateException("Attempted to use a configuration ordering without providing configuration")
}

class SemVerVersionOrdering : VersionsOrdering {
    override fun order(records: List<VersionId>): List<VersionId> =
        records.map { it to ComparableVersion(it) }.sortedByDescending { it.second }.map { it.first }
}
