/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.adapters

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.adapters.KotlinAdapter.Companion.currentKotlinToolingVersion
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.engine.parameters.SourceSetIdSpec
import org.jetbrains.dokka.gradle.engine.parameters.SourceSetIdSpec.Companion.dokkaSourceSetIdSpec
import org.jetbrains.dokka.gradle.internal.*
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File
import javax.inject.Inject
import kotlin.reflect.jvm.jvmName

/**
 * The [KotlinAdapter] plugin will automatically register Kotlin source sets as Dokka source sets.
 *
 * This is an internal Dokka plugin and should not be used externally.
 * It is not a standalone plugin, it requires [org.jetbrains.dokka.gradle.DokkaBasePlugin] is also applied.
 */
@InternalDokkaGradlePluginApi
abstract class KotlinAdapter @Inject constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : Plugin<Project> {

    override fun apply(project: Project) {
        logger.info("Applying $dkaName to ${project.path}")

        project.plugins.withType<DokkaBasePlugin>().configureEach {
            project.pluginManager.apply {
                withPlugin(PluginId.KotlinAndroid) { exec(project) }
                withPlugin(PluginId.KotlinJs) { exec(project) }
                withPlugin(PluginId.KotlinJvm) { exec(project) }
                withPlugin(PluginId.KotlinMultiplatform) { exec(project) }
            }
        }
    }

    private fun exec(project: Project) {
        val kotlinExtension = project.extensions.findKotlinExtension()
        if (kotlinExtension == null) {
            if (project.extensions.findByName("kotlin") != null) {
                // uh oh - the Kotlin extension is present but findKotlinExtension() failed.
                // Is there a class loader issue? https://github.com/gradle/gradle/issues/27218
                logger.warn {
                    val allPlugins =
                        project.plugins.joinToString { it::class.qualifiedName ?: "${it::class}" }
                    val allExtensions =
                        project.extensions.extensionsSchema.elements.joinToString { "${it.name} ${it.publicType}" }

                    /* language=TEXT */
                    """
                    |$dkaName failed to get KotlinProjectExtension in ${project.path}
                    |  Applied plugins: $allPlugins
                    |  Available extensions: $allExtensions
                    """.trimMargin()
                }
            }
            logger.info("Skipping applying $dkaName in ${project.path} - could not find KotlinProjectExtension")
            return
        }
        logger.info("Configuring $dkaName in Gradle Kotlin Project ${project.path}")

        val dokkaExtension = project.extensions.getByType<DokkaExtension>()

        // first fetch the relevant properties of all KotlinCompilations
        val compilationDetailsBuilder = KotlinCompilationDetailsBuilder(
            providers = providers,
            objects = objects,
            konanHome = dokkaExtension.konanHome.asFile,
            project = project,
        )
        val allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails> =
            compilationDetailsBuilder.createCompilationDetails(
                kotlinProjectExtension = kotlinExtension,
            )

        // second, fetch the relevant properties of the Kotlin source sets
        val sourceSetDetailsBuilder = KotlinSourceSetDetailsBuilder(
            providers = providers,
            objects = objects,
            sourceSetScopeDefault = dokkaExtension.sourceSetScopeDefault,
            projectPath = project.path,
        )
        val sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails> =
            sourceSetDetailsBuilder.createSourceSetDetails(
                kotlinSourceSets = kotlinExtension.sourceSets,
                allKotlinCompilationDetails = allKotlinCompilationDetails,
            )

        // for each Kotlin source set, register a Dokka source set
        registerDokkaSourceSets(
            dokkaExtension = dokkaExtension,
            sourceSetDetails = sourceSetDetails,
        )
    }

    /** Register a [DokkaSourceSetSpec] for each element in [sourceSetDetails] */
    private fun registerDokkaSourceSets(
        dokkaExtension: DokkaExtension,
        sourceSetDetails: NamedDomainObjectContainer<KotlinSourceSetDetails>,
    ) {
        // proactively use 'all' so source sets will be available in users' build files if they use `named("...")`
        sourceSetDetails.all details@{
            dokkaExtension.dokkaSourceSets.register(details = this@details)
        }
    }

    /** Register a single [DokkaSourceSetSpec] for [details]. */
    private fun NamedDomainObjectContainer<DokkaSourceSetSpec>.register(
        details: KotlinSourceSetDetails
    ) {
        val kssPlatform = details.compilations.map { values: List<KotlinCompilationDetails> ->
            val all = values.map { it.kotlinPlatform }.distinct()
            val r = all.singleOrNull()

            println("[$dkaName] computed platform for ${details.name}: $r (from ${all})")

            r ?: KotlinPlatform.Common
        }

        val kssClasspath = determineClasspath(details)

        register(details.name) dss@{
            suppress.set(!details.isPublishedSourceSet())
            analysisPlatform.set(kssPlatform)
//            suppress.convention(!details.isPublishedSourceSet())
//            analysisPlatform.convention(kssPlatform)

            sourceRoots.from(details.sourceDirectories)
            classpath.from(kssClasspath)
            dependentSourceSets.addAllLater(details.dependentSourceSetIds)
        }
    }

    private fun determineClasspath(
        details: KotlinSourceSetDetails
    ): Provider<FileCollection> {
        return details.compilations.map { compilations: List<KotlinCompilationDetails> ->
            val classpath = objects.fileCollection()

            if (compilations.isNotEmpty()) {
                compilations.fold(classpath) { acc, compilation ->
                    acc.from(compilation.compilationClasspath)
                }
            } else {
                classpath
                    .from(details.sourceDirectories)
                    .from(details.sourceDirectoriesOfDependents)
            }
        }
    }

    @InternalDokkaGradlePluginApi
    companion object {
        private val dkaName: String = KotlinAdapter::class.simpleName!!

        private val logger = Logging.getLogger(KotlinAdapter::class.java)

        /** Try and get [KotlinProjectExtension], or `null` if it's not present */
        private fun ExtensionContainer.findKotlinExtension(): KotlinProjectExtension? =
            try {
                findByType()
                // fallback to trying to get the JVM extension
                // (not sure why I did this... maybe to be compatible with really old versions?)
                    ?: findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()
            } catch (e: Throwable) {
                when (e) {
                    is TypeNotPresentException,
                    is ClassNotFoundException,
                    is NoClassDefFoundError -> {
                        logger.info("$dkaName failed to find KotlinExtension ${e::class} ${e.message}")
                        null
                    }

                    else -> throw e
                }
            }

        /** Get the version of the Kotlin Gradle Plugin currently used to compile the project */
        // Must be lazy, else tests fail (because the KGP plugin isn't accessible)
        internal val currentKotlinToolingVersion: KotlinToolingVersion by lazy {
            val kgpVersion = getKotlinPluginVersion(logger)
            KotlinToolingVersion(kgpVersion)
        }
    }
}


/**
 * Store the details of all [KotlinCompilation]s in a configuration cache compatible way.
 *
 * The compilation details may come from a multiplatform project ([KotlinMultiplatformExtension])
 * or a single-platform project ([KotlinSingleTargetExtension]).
 */
@InternalDokkaGradlePluginApi
private data class KotlinCompilationDetails(
    val target: String,
    val kotlinPlatform: KotlinPlatform,
    val allKotlinSourceSetsNames: Set<String>,
    val publishedCompilation: Boolean,
    val dependentSourceSetNames: Set<String>,
    val compilationClasspath: FileCollection,
    val defaultSourceSetName: String,
)


/** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
private class KotlinCompilationDetailsBuilder(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val konanHome: Provider<File>,
    private val project: Project,
) {

    fun createCompilationDetails(
        kotlinProjectExtension: KotlinProjectExtension,
    ): ListProperty<KotlinCompilationDetails> {

        val details = objects.listProperty<KotlinCompilationDetails>()

        details.addAll(
            providers.provider {
                kotlinProjectExtension
                    .allKotlinCompilations()
                    .map { compilation ->
                        createCompilationDetails(compilation = compilation)
                    }
            })

        return details
    }

    /** Create a single [KotlinCompilationDetails] for [compilation] */
    private fun createCompilationDetails(
        compilation: KotlinCompilation<*>,
    ): KotlinCompilationDetails {
        val allKotlinSourceSetsNames =
            compilation.allKotlinSourceSets.map { it.name } + compilation.defaultSourceSet.name

        val dependentSourceSetNames =
            compilation.defaultSourceSet.dependsOn.map { it.name }

        val compilationClasspath: FileCollection =
            collectKotlinCompilationClasspath(compilation = compilation)

        return KotlinCompilationDetails(
            target = compilation.target.name,
            kotlinPlatform = KotlinPlatform.fromString(compilation.platformType.name),
            allKotlinSourceSetsNames = allKotlinSourceSetsNames.toSet(),
            publishedCompilation = compilation.isPublished(),
            dependentSourceSetNames = dependentSourceSetNames.toSet(),
            compilationClasspath = compilationClasspath,
            defaultSourceSetName = compilation.defaultSourceSet.name
        )
    }

    private fun KotlinProjectExtension.allKotlinCompilations(): Collection<KotlinCompilation<*>> =
        when (this) {
            is KotlinMultiplatformExtension -> targets.flatMap { it.compilations }
            is KotlinSingleTargetExtension<*> -> target.compilations
            else -> emptyList() // shouldn't happen?
        }

    /**
     * Get the [Configuration][org.gradle.api.artifacts.Configuration] names of all configurations
     * used to build this [KotlinCompilation] and
     * [its source sets][KotlinCompilation.kotlinSourceSets].
     */
    private fun collectKotlinCompilationClasspath(
        compilation: KotlinCompilation<*>,
    ): FileCollection {
        val compilationClasspath = objects.fileCollection()

        compilationClasspath.from(
            kotlinCompileDependencyFiles(compilation)
        )

        compilationClasspath.from(
            kotlinNativeDependencies(compilation)
        )

        return compilationClasspath
    }

    private fun kotlinCompileDependencyFiles(
        compilation: KotlinCompilation<*>,
    ): Provider<FileCollection> {
        return project.configurations
            .named(compilation.compileDependencyConfigurationName)
            .map {
                it.incoming
                    .artifactView {
                        // Android publishes many variants, which can cause Gradle to get confused,
                        // so specify that we need a JAR and resolve leniently
                        if (compilation.target.platformType == KotlinPlatformType.androidJvm) {
                            attributes { artifactType("jar") }
                            lenient(true)
                        }
                        // 'Regular' Kotlin compilations have non-JAR files (e.g. Kotlin/Native klibs),
                        // so don't add attributes for non-Android projects.
                    }
                    .artifacts
                    .artifactFiles
            }
    }

    private fun kotlinNativeDependencies(
        compilation: KotlinCompilation<*>,
    ): Provider<FileCollection> {

        // apply workaround for Kotlin/Native, which will be fixed in Kotlin 2.0
        // (see KT-61559: K/N dependencies will be part of `compilation.compileDependencyFiles`)
        return if (
            currentKotlinToolingVersion < KotlinToolingVersion("2.0.0")
            &&
            compilation is AbstractKotlinNativeCompilation
        ) {
            konanHome.map { konanHome ->
                val konanDistribution = KonanDistribution(konanHome)

                val dependencies = objects.fileCollection()

                dependencies.from(konanDistribution.stdlib)

                // Konan library files for a specific target
                dependencies.from(
                    konanDistribution.platformLibsDir
                        .resolve(compilation.target.name)
                        .listFiles()
                        .orEmpty()
                        .filter { it.isDirectory || it.extension == "klib" }
                )
            }
        } else {
            return providers.provider { objects.fileCollection() }
        }
    }

    companion object {

        /**
         * Determine if a [KotlinCompilation] is 'publishable', and so should be enabled by default
         * when creating a Dokka publication.
         *
         * Typically, 'main' compilations are publishable and 'test' compilations should be suppressed.
         * This can be overridden manually, though.
         *
         * @see DokkaSourceSetSpec.suppress
         */
        private fun KotlinCompilation<*>.isPublished(): Boolean {
            return when (this) {
                is KotlinMetadataCompilation<*> -> true

                is KotlinJvmAndroidCompilation -> {
                    // Use string-based comparison, not the actual classes, because AGP has deprecated and
                    // moved the Library/Application classes to a different package.
                    // Using strings is more widely compatible.
                    val variantName = androidVariant::class.jvmName
                    "LibraryVariant" in variantName || "ApplicationVariant" in variantName
                }

                else ->
                    name == MAIN_COMPILATION_NAME
            }
        }
    }
}


/**
 * Store the details of all [KotlinSourceSet]s in a configuration cache compatible way.
 *
 * @param[named] Should be [KotlinSourceSet.getName]
 */
@InternalDokkaGradlePluginApi
private abstract class KotlinSourceSetDetails @Inject constructor(
    private val named: String,
) : Named {

    /** Direct source sets that this source set depends on */
    abstract val dependentSourceSetIds: SetProperty<SourceSetIdSpec>
    abstract val sourceDirectories: ConfigurableFileCollection

    /** _All_ source directories from any (recursively) dependant source set */
    abstract val sourceDirectoriesOfDependents: ConfigurableFileCollection

    /** The specific compilations used to build this source set */
    abstract val compilations: ListProperty<KotlinCompilationDetails>

    /** Estimate if this Kotlin source set contains 'published' sources */
    fun isPublishedSourceSet(): Provider<Boolean> =
        compilations.map { values ->
            values.any { it.publishedCompilation }
        }

    override fun getName(): String = named
}


/** Utility class, encapsulating logic for building [KotlinCompilationDetails] */
private class KotlinSourceSetDetailsBuilder(
    private val sourceSetScopeDefault: Provider<String>,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    /** Used for logging */
    private val projectPath: String,
) {

    private val logger = Logging.getLogger(KotlinSourceSetDetails::class.java)

    fun createSourceSetDetails(
        kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
        allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
    ): NamedDomainObjectContainer<KotlinSourceSetDetails> {

        val sourceSetDetails = objects.domainObjectContainer(KotlinSourceSetDetails::class)

        kotlinSourceSets.configureEach kss@{
            sourceSetDetails.register(
                kotlinSourceSet = this,
                allKotlinCompilationDetails = allKotlinCompilationDetails,
            )
        }

        return sourceSetDetails
    }

    private fun NamedDomainObjectContainer<KotlinSourceSetDetails>.register(
        kotlinSourceSet: KotlinSourceSet,
        allKotlinCompilationDetails: ListProperty<KotlinCompilationDetails>,
    ) {

        // TODO: Needs to respect filters.
        //  We probably need to change from "sourceRoots" to support "sourceFiles"
        //  https://github.com/Kotlin/dokka/issues/1215
        val extantSourceDirectories = providers.provider {
            kotlinSourceSet.kotlin.sourceDirectories.filter { it.exists() }
        }

        val compilations = allKotlinCompilationDetails.map { allCompilations ->
            allCompilations.filter { compilation ->
                kotlinSourceSet.name in compilation.allKotlinSourceSetsNames
            }
        }

        // determine the source sets IDs of _other_ source sets that _this_ source depends on.
        val dependentSourceSets = providers.provider { kotlinSourceSet.dependsOn }
        val dependentSourceSetIds =
            providers.zip(
                dependentSourceSets,
                sourceSetScopeDefault,
            ) { sourceSets, sourceSetScope ->
                logger.info("[$projectPath] source set ${kotlinSourceSet.name} has ${sourceSets.size} dependents ${sourceSets.joinToString { it.name }}")
                sourceSets.map { dependedKss ->
                    objects.dokkaSourceSetIdSpec(sourceSetScope, dependedKss.name)
                }
            }

        val sourceDirectoriesOfDependents = providers.provider {
            kotlinSourceSet
                .allDependentSourceSets()
                .fold(objects.fileCollection()) { acc, sourceSet ->
                    acc.from(sourceSet.kotlin.sourceDirectories)
                }
        }

        register(kotlinSourceSet.name) {
            this.dependentSourceSetIds.addAll(dependentSourceSetIds)
            this.sourceDirectories.from(extantSourceDirectories)
            this.sourceDirectoriesOfDependents.from(sourceDirectoriesOfDependents)
            this.compilations.addAll(compilations)
        }
    }


    /**
     * Return a list containing _all_ source sets that this source set depends on,
     * searching recursively.
     *
     * @see KotlinSourceSet.dependsOn
     */
    private tailrec fun KotlinSourceSet.allDependentSourceSets(
        queue: Set<KotlinSourceSet> = dependsOn.toSet(),
        allDependents: List<KotlinSourceSet> = emptyList(),
    ): List<KotlinSourceSet> {
        val next = queue.firstOrNull() ?: return allDependents
        return next.allDependentSourceSets(
            queue = (queue - next) union next.dependsOn,
            allDependents = allDependents + next,
        )
    }
}
