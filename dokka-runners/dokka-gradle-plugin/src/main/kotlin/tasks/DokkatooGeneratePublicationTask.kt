/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.gradle.internal.DokkaPluginParametersContainer
import org.jetbrains.dokka.gradle.internal.DokkatooInternalApi
import javax.inject.Inject


/**
 * Generate a complete Dokka Publication.
 *
 * A Publication may contain zero-to-many Dokka Modules, which are generated by
 * [DokkatooGenerateModuleTask].
 */
@CacheableTask
abstract class DokkatooGeneratePublicationTask
@DokkatooInternalApi
@Inject
constructor(
    objects: ObjectFactory,
    workers: WorkerExecutor,
    archives: ArchiveOperations,

    private val fs: FileSystemOperations,
    /**
     * Configurations for Dokka Generator Plugins. Must be provided from
     * [org.jetbrains.dokka.gradle.dokka.DokkaPublication.pluginsConfiguration].
     */
    pluginsConfiguration: DokkaPluginParametersContainer,
) : DokkatooGenerateTask(
    objects = objects,
    workers = workers,
    pluginsConfiguration = pluginsConfiguration,
    archives = archives,
) {

    @TaskAction
    internal fun generatePublication() {
        val outputDirectory = outputDirectory.get().asFile

        // clean output dir, so previous generations don't dirty this generation
        fs.delete { delete(outputDirectory) }
        outputDirectory.mkdirs()

        // run Dokka Generator
        generateDocumentation(GeneratorMode.Publication, outputDirectory)
    }
}
