/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.tasks

import kotlinx.serialization.json.JsonElement
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.gradle.DokkaBasePlugin.Companion.jsonMapper
import org.jetbrains.dokka.gradle.engine.parameters.DokkaGeneratorParametersSpec
import org.jetbrains.dokka.gradle.engine.parameters.builders.DokkaParametersBuilder
import org.jetbrains.dokka.gradle.internal.DokkaPluginParametersContainer
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.workers.ClassLoaderIsolation
import org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker
import org.jetbrains.dokka.gradle.workers.ProcessIsolation
import org.jetbrains.dokka.gradle.workers.WorkerIsolation
import org.jetbrains.dokka.toPrettyJsonString
import java.io.File
import javax.inject.Inject

/**
 * Base task for executing Dokka Generator, producing documentation.
 *
 * The Dokka Plugins added to the generator classpath determine the type of documentation generated.
 */
@CacheableTask
abstract class DokkaGenerateTask
@InternalDokkaGradlePluginApi
@Inject
constructor(
    objects: ObjectFactory,
    archives: ArchiveOperations,
    private val workers: WorkerExecutor,

    /**
     * Configurations for Dokka Generator Plugins. Must be provided from
     * [org.jetbrains.dokka.gradle.formats.DokkaPublication.pluginsConfiguration].
     */
    pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkaBaseTask() {

    private val dokkaParametersBuilder = DokkaParametersBuilder(archives)

    /**
     * Directory containing the generation result. The content and structure depends on whether
     * the task generates a Dokka Module or a Dokka Publication.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * Classpath required to run Dokka Generator.
     *
     * Contains the Dokka Generator, Dokka plugins, and any transitive dependencies.
     */
    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    /** @see org.jetbrains.dokka.gradle.formats.DokkaPublication.cacheRoot */
    @get:LocalState
    abstract val cacheDirectory: DirectoryProperty

    /** @see org.jetbrains.dokka.gradle.formats.DokkaPublication.enabled */
    @get:Input
    abstract val publicationEnabled: Property<Boolean>

    @get:Nested
    val generator: DokkaGeneratorParametersSpec = objects.newInstance(pluginsConfiguration)

    /**
     * Control how Dokka Gradle Plugin launches Dokka Generator.
     *
     * Defaults to [org.jetbrains.dokka.gradle.DokkaExtension.dokkaGeneratorIsolation].
     *
     * @see org.jetbrains.dokka.gradle.DokkaExtension.dokkaGeneratorIsolation
     * @see org.jetbrains.dokka.gradle.workers.ProcessIsolation
     */
    @get:Nested
    abstract val workerIsolation: Property<WorkerIsolation>

    /**
     * All [org.jetbrains.dokka.DokkaGenerator] logs will be saved to this file.
     * This can be used for debugging purposes.
     */
    @get:Internal
    abstract val workerLogFile: RegularFileProperty

    /**
     * The [DokkaConfiguration] by Dokka Generator can be saved to a file for debugging purposes.
     * To disable this behaviour set this property to `null`.
     */
    @InternalDokkaGradlePluginApi
    @get:Internal
    abstract val dokkaConfigurationJsonFile: RegularFileProperty

    /**
     * Completely override the default Dokka configuration with JSON encoded
     * Dokka Configuration.
     *
     * This should only be used for local debugging.
     */
    @get:Input
    @get:Optional
    @InternalDokkaGradlePluginApi
    abstract val overrideJsonConfig: Property<String>

    @InternalDokkaGradlePluginApi
    enum class GeneratorMode {
        Module,
        Publication,
    }

    @InternalDokkaGradlePluginApi
    protected fun generateDocumentation(
        generationType: GeneratorMode,
        outputDirectory: File,
    ) {
        val dokkaConfiguration =
            if (overrideJsonConfig.isPresent) {
                logger.warn("w: [$path] Overriding DokkaConfiguration with overrideJsonConfig")
                DokkaConfigurationImpl(overrideJsonConfig.get())
            } else {
                createDokkaConfiguration(generationType, outputDirectory)
            }

        logger.info("dokkaConfiguration: $dokkaConfiguration")
        verifyDokkaConfiguration(dokkaConfiguration)
        dumpDokkaConfigurationJson(dokkaConfiguration)

        logger.info("DokkaGeneratorWorker runtimeClasspath: ${runtimeClasspath.asPath}")

        val isolation = workerIsolation.get()
        logger.info("[$path] running with workerIsolation $isolation")
        val workQueue = when (isolation) {
            is ClassLoaderIsolation ->
                workers.classLoaderIsolation {
                    classpath.from(runtimeClasspath)
                }

            is ProcessIsolation ->
                workers.processIsolation {
                    classpath.from(runtimeClasspath)
                    forkOptions {
                        isolation.defaultCharacterEncoding.orNull?.let(this::setDefaultCharacterEncoding)
                        isolation.debug.orNull?.let(this::setDebug)
                        isolation.enableAssertions.orNull?.let(this::setEnableAssertions)
                        isolation.maxHeapSize.orNull?.let(this::setMaxHeapSize)
                        isolation.minHeapSize.orNull?.let(this::setMinHeapSize)
                        isolation.jvmArgs.orNull?.filter { it.isNotBlank() }?.let(this::setJvmArgs)
                        isolation.systemProperties.orNull?.let(this::systemProperties)
                    }
                }
        }

        workQueue.submit(DokkaGeneratorWorker::class) {
            this.dokkaParameters.set(dokkaConfiguration)
            this.logFile.set(workerLogFile)
            this.taskPath.set(this@DokkaGenerateTask.path)
        }
    }

    /**
     * Run some helper checks to log warnings if the [DokkaConfiguration] looks misconfigured.
     */
    private fun verifyDokkaConfiguration(dokkaConfiguration: DokkaConfiguration) {
        val modulesWithDuplicatePaths = dokkaConfiguration.modules
            .groupBy { it.relativePathToOutputDirectory.toString() }
            .filterValues { it.size > 1 }

        if (modulesWithDuplicatePaths.isNotEmpty()) {
            val modulePaths = modulesWithDuplicatePaths.entries
                .map { (path, modules) ->
                    "${modules.joinToString { "'${it.name}'" }} have modulePath '$path'"
                }
                .sorted()
                .joinToString("\n") { "  - $it" }
            logger.warn(
                """
                |[$path] Duplicate `modulePath`s in Dokka Generator parameters.
                |  modulePaths must be distinct for each module, as they are used to determine the output
                |  directory. Duplicates mean Dokka Generator may overwrite one module with another.
                |$modulePaths
                |
                """.trimMargin()
            )
        }
    }

    /**
     * Dump the [DokkaConfiguration] JSON to a file ([dokkaConfigurationJsonFile]) for debugging
     * purposes.
     */
    private fun dumpDokkaConfigurationJson(
        dokkaConfiguration: DokkaConfiguration,
    ) {
        val destFile = dokkaConfigurationJsonFile.asFile.orNull ?: return
        destFile.parentFile.mkdirs()
        destFile.createNewFile()

        val compactJson = dokkaConfiguration.toPrettyJsonString()
        val json = jsonMapper.decodeFromString(JsonElement.serializer(), compactJson)
        val prettyJson = jsonMapper.encodeToString(JsonElement.serializer(), json)

        destFile.writeText(prettyJson)

        logger.info("[$path] Dokka Generator configuration JSON: ${destFile.toURI()}")
    }

    private fun createDokkaConfiguration(
        generationType: GeneratorMode,
        outputDirectory: File,
    ): DokkaConfiguration {

        val delayTemplateSubstitution = when (generationType) {
            GeneratorMode.Module -> true
            GeneratorMode.Publication -> false
        }

        val moduleOutputDirectories = generator.moduleOutputDirectories.toList()
        logger.info("[$path] got ${moduleOutputDirectories.size} moduleOutputDirectories: $moduleOutputDirectories")

        return dokkaParametersBuilder.build(
            spec = generator,
            delayTemplateSubstitution = delayTemplateSubstitution,
            outputDirectory = outputDirectory,
            moduleDescriptorDirs = moduleOutputDirectories,
            cacheDirectory = cacheDirectory.asFile.orNull,
        )
    }
}
