package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import java.io.File

internal fun GradleDokkaSourceSetBuilder.toDokkaSourceSetImpl(): DokkaSourceSetImpl = DokkaSourceSetImpl(
    classpath = classpath.toList(),
    displayName = displayNameOrDefault(),
    sourceSetID = sourceSetID,
    sourceRoots = sourceRoots.toSet(),
    dependentSourceSets = dependentSourceSets.get(),
    samples = samples.toSet(),
    includes = includes.toSet(),
    includeNonPublic = includeNonPublic.get(),
    documentedVisibilities = documentedVisibilities.get(),
    reportUndocumented = reportUndocumented.get(),
    skipEmptyPackages = skipEmptyPackages.get(),
    skipDeprecated = skipDeprecated.get(),
    jdkVersion = jdkVersion.get(),
    sourceLinks = sourceLinks.get().build().toSet(),
    perPackageOptions = perPackageOptions.get().build(),
    externalDocumentationLinks = externalDocumentationLinksWithDefaults(),
    languageVersion = languageVersion.orNull,
    apiVersion = apiVersion.orNull,
    noStdlibLink = noStdlibLink.get(),
    noJdkLink = noJdkLink.get(),
    suppressedFiles = suppressedFilesWithDefaults(),
    analysisPlatform = platform.get()
)

private fun GradleDokkaSourceSetBuilder.displayNameOrDefault(): String {
    return displayName.getOrElse(
        if (name.endsWith("Main") && name != "Main") {
            name.removeSuffix("Main")
        } else {
            name
        }
    )
}

private fun GradleDokkaSourceSetBuilder.externalDocumentationLinksWithDefaults(): Set<ExternalDocumentationLinkImpl> {
    return externalDocumentationLinks.get().build()
        .run {
            if (noJdkLink.get()) this
            else this + ExternalDocumentationLink.jdk(jdkVersion.get())
        }
        .run {
            if (noStdlibLink.get()) this
            else this + ExternalDocumentationLink.kotlinStdlib()
        }
        .run {
            if (noAndroidSdkLink.get() || !project.isAndroidProject()) this
            else this +
                    ExternalDocumentationLink.androidSdk() +
                    ExternalDocumentationLink.androidX()
        }
        .toSet()
}

private fun GradleDokkaSourceSetBuilder.suppressedFilesWithDefaults(): Set<File> {
    val suppressedGeneratedFiles = if (suppressGeneratedFiles.get()) {
        val generatedRoot = project.buildDir.resolve("generated").absoluteFile
        sourceRoots
            .filter { it.startsWith(generatedRoot) }
            .flatMap { it.walk().toList() }
            .toSet()
    } else {
        emptySet()
    }

    return suppressedFiles.toSet() + suppressedGeneratedFiles
}
