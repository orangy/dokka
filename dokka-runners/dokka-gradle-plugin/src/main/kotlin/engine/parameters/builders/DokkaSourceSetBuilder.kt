/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters.builders

import org.gradle.api.logging.Logging
import org.jetbrains.dokka.*
import org.jetbrains.dokka.gradle.engine.parameters.*
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform.Companion.dokkaType
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Companion.dokkaType
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.mapNotNullToSet
import org.jetbrains.dokka.gradle.internal.mapToSet

/**
 * Convert the Gradle-focused [DokkaSourceSetSpec] into a [DokkaSourceSetImpl] instance, which
 * will be passed to Dokka Generator.
 *
 * The conversion is defined in a separate class to try and prevent classes from Dokka Generator
 * leaking into the public API.
 */
@InternalDokkaGradlePluginApi
internal object DokkaSourceSetBuilder {

    private val logger = Logging.getLogger(DokkaParametersBuilder::class.java)

    fun buildAll(sourceSets: Set<DokkaSourceSetSpec>): List<DokkaSourceSetImpl> {

        val suppressedSourceSetIds = sourceSets.mapNotNullToSet {
            val suppressed = it.suppress.get()
            val sourceSetId = it.sourceSetId.get()
            if (suppressed) {
                logger.info("Dokka source set $sourceSetId is suppressed")
                sourceSetId
            } else {
                logger.info("Dokka source set $sourceSetId isn't suppressed")
                null
            }
        }

        val enabledSourceSets = sourceSets.filter { it.sourceSetId.get() !in suppressedSourceSetIds }

        return enabledSourceSets
            .map { build(it, suppressedSourceSetIds) }
            // Sort the source sets, just to help with debugging and investigations when looking at the JSON config file.
            // The order shouldn't have any impact on Dokka Generator.
            .sortedBy { it.sourceSetID.toString() }
    }

    private fun build(
        spec: DokkaSourceSetSpec,
        suppressedSourceSetIds: Set<SourceSetIdSpec>,
    ): DokkaSourceSetImpl {

        val dependentSourceSets =
            (spec.dependentSourceSets subtract suppressedSourceSetIds).mapToSet(::build)

        return DokkaSourceSetImpl(
            // properties
            analysisPlatform = spec.analysisPlatform.get().dokkaType,
            apiVersion = spec.apiVersion.orNull,
            dependentSourceSets = dependentSourceSets,
            displayName = spec.displayName.get(),
            documentedVisibilities = spec.documentedVisibilities.get().mapToSet { it.dokkaType },
            externalDocumentationLinks = spec.externalDocumentationLinks.mapNotNullToSet(::build),
            jdkVersion = spec.jdkVersion.get(),
            languageVersion = spec.languageVersion.orNull,
            noJdkLink = !spec.enableJdkDocumentationLink.get(),
            noStdlibLink = !spec.enableKotlinStdLibDocumentationLink.get(),
            perPackageOptions = spec.perPackageOptions.map(::build),
            reportUndocumented = spec.reportUndocumented.get(),
            skipDeprecated = spec.skipDeprecated.get(),
            skipEmptyPackages = spec.skipEmptyPackages.get(),
            sourceLinks = spec.sourceLinks.mapToSet { build(it) },
            sourceSetID = build(spec.sourceSetId.get()),

            // files
            classpath = spec.classpath.asFileTree.files.toList(),
            includes = spec.includes.asFileTree.files,
            samples = spec.samples.asFileTree.files,
            sourceRoots = spec.sourceRoots.files,
            suppressedFiles = spec.suppressedFiles.asFileTree.files,
        )
    }

    private fun build(spec: DokkaExternalDocumentationLinkSpec): ExternalDocumentationLinkImpl? {
        if (!spec.enabled.getOrElse(true)) return null

        return ExternalDocumentationLinkImpl(
            url = spec.url.get().toURL(),
            packageListUrl = spec.packageListUrl.get().toURL(),
        )
    }

    private fun build(spec: DokkaPackageOptionsSpec): PackageOptionsImpl =
        PackageOptionsImpl(
            matchingRegex = spec.matchingRegex.get(),
            documentedVisibilities = spec.documentedVisibilities.get().mapToSet { it.dokkaType },
            reportUndocumented = spec.reportUndocumented.get(),
            skipDeprecated = spec.skipDeprecated.get(),
            suppress = spec.suppress.get(),
            includeNonPublic = DokkaDefaults.includeNonPublic,
        )

    private fun build(spec: SourceSetIdSpec): DokkaSourceSetID =
        DokkaSourceSetID(
            scopeId = spec.scopeId,
            sourceSetName = spec.sourceSetName
        )

    private fun build(spec: DokkaSourceLinkSpec): SourceLinkDefinitionImpl =
        SourceLinkDefinitionImpl(
            localDirectory = spec.localDirectory.asFile.get().invariantSeparatorsPath,
            remoteUrl = spec.remoteUrl.get().toURL(),
            remoteLineSuffix = spec.remoteLineSuffix.orNull,
        )
}
