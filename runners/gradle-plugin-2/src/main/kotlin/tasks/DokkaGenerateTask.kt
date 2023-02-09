package org.jetbrains.dokka.gradle.tasks

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.jsonMapper
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs
import org.jetbrains.dokka.gradle.workers.DokkaGeneratorWorker
import javax.inject.Inject

/**
 * Executes the Dokka Generator, and produces documentation.
 *
 * The type of documentation generated is determined by the supplied Dokka Plugins in [pluginClasspath].
 */
@CacheableTask
abstract class DokkaGenerateTask @Inject constructor(
    private val providers: ProviderFactory,
    private val workers: WorkerExecutor,
) : DokkaTask() {

    @get:InputFile
    @get:PathSensitive(NAME_ONLY)
    abstract val dokkaConfigurationJson: RegularFileProperty

    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

//    @get:Classpath
//    abstract val pluginClasspath: ConfigurableFileCollection

    @get:LocalState
    abstract val cacheDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    @OptIn(ExperimentalSerializationApi::class) // jsonMapper.decodeFromStream
    fun generateDocumentation() {
        val dokkaConfigurationJsonFile = dokkaConfigurationJson.get().asFile

        val dokkaConfiguration = jsonMapper.decodeFromStream(
            DokkaConfigurationKxs.serializer(),
            dokkaConfigurationJsonFile.inputStream(),
        ).copy(
            outputDir = outputDirectory.get().asFile,
        )

        logger.info("dokkaConfiguration: $dokkaConfiguration")

        logger.info("DokkaGeneratorWorker runtimeClasspath: ${runtimeClasspath.files.joinToString("\n") { it.name }}")

        val workQueue = workers.processIsolation {
            classpath.from(runtimeClasspath)
//            classpath.from(pluginClasspath)
            forkOptions {
                defaultCharacterEncoding = "UTF-8"
            }
        }

        workQueue.submit(DokkaGeneratorWorker::class) worker@{
            this@worker.dokkaConfiguration.set(dokkaConfiguration)
        }
    }

//    /**
//     * Extract a property from [dokkaConfigurationJson] so it can be converted to a specific Gradle property type
//     * (for example, a [DirectoryProperty]) and used as a task property.
//     */
//    internal inline fun <reified T : Any> dokkaConfigurationValue(
//        crossinline property: DokkaConfigurationKxs.() -> T?
//    ): Provider<T> {
//        return providers.fileContents(dokkaConfigurationJson).asText.mapNotNull { dokkaConfigurationJson ->
//
//            val dokkaConfiguration = jsonMapper.decodeFromString(
//                DokkaConfigurationKxs.serializer(),
//                dokkaConfigurationJson,
//            )
//
//            dokkaConfiguration.property()
//        }
//    }
//
//    // workaround for https://github.com/gradle/gradle/issues/12388
//    private fun <T, R> Provider<T>.mapNotNull(map: (value: T) -> R?): Provider<R> =
//        flatMap { value -> providers.provider { map(value) } }
}
