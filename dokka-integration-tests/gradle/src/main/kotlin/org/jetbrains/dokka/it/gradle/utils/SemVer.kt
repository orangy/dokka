/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.utils

/**
 * A SemVer version.
 */
data class SemVer(
    val version: String,
) : Comparable<SemVer> {
    val major: Int

    //region These fields are private to avoid IJ warnings, feel free to make them public if it's helpful.
    private val minor: Int
    val patch: Int
    private val prerelease: String?
    private val metadata: String?
    //endregion

    init {
        val match = semverRegex.matchEntire(version) ?: error("Invalid version '$version'")
        major = match.groups["major"]?.value?.toIntOrNull() ?: error("missing major version in '$version'")
        minor = match.groups["minor"]?.value?.toIntOrNull() ?: error("missing minor version in '$version'")
        patch = match.groups["patch"]?.value?.toIntOrNull() ?: error("missing patch version in '$version'")
        prerelease = match.groups["prerelease"]?.value
        metadata = match.groups["buildMetadata"]?.value
    }

    override fun compareTo(other: SemVer): Int {
        return when {
            this.version == other.version -> 0
            this.major != other.major -> this.major.compareTo(other.major)
            this.minor != other.minor -> this.minor.compareTo(other.minor)
            this.patch != other.patch -> this.patch.compareTo(other.patch)
            this.prerelease != other.prerelease -> {
                when {
                    this.prerelease == null -> 1
                    other.prerelease == null -> -1
                    else -> this.prerelease.compareTo(other.prerelease)
                }
            }

            this.metadata != other.metadata -> {
                when {
                    this.metadata == null -> 1
                    other.metadata == null -> -1
                    else -> this.metadata.compareTo(other.metadata)
                }
            }

            else -> {
                0
            }
        }
    }

    /** Combine the [major] and [minor] versions into a string. */
    val majorAndMinorVersions: String = "$major.$minor"

    override fun toString(): String = version

    companion object {
        // https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
        internal val semverRegex = Regex(
            """
            ^(?<major>0|[1-9]\d*)\.(?<minor>0|[1-9]\d*)\.(?<patch>0|[1-9]\d*)(?:-(?<prerelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+(?<buildMetadata>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?${'$'}
            """.trimIndent()
        )

        operator fun ClosedRange<String>.contains(version: SemVer): Boolean =
            version in SemVer(start)..SemVer(endInclusive)

        operator fun OpenEndRange<String>.contains(version: SemVer): Boolean =
            version in SemVer(start)..<SemVer(endExclusive)
    }
}
