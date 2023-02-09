package org.jetbrains.dokka.gradle.tasks

import kotlinx.serialization.encodeToString
import org.gradle.api.DomainObjectSet
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.dokka.gradle.DokkaPlugin.Companion.jsonMapper
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaPluginConfigurationGradleBuilder
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaSourceSetGradleBuilder
import javax.inject.Inject

/**
 * Builds the Dokka Configuration that the Dokka Generator will use to produce a Dokka Publication for this project.
 *
 * Configurations from other modules (which are potentially from other Gradle subprojects) will also be included
 * via ... TODO explain how to include other subprojects/modules
 */
@CacheableTask
abstract class DokkaConfigurationTask @Inject constructor(
    objects: ObjectFactory,
) : DokkaTask() {

    @get:OutputFile
    abstract val dokkaConfigurationJson: RegularFileProperty

    /** Dokka Configuration files from other subprojects that will be merged into this Dokka Configuration */
    @get:InputFiles
//    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val dokkaSubprojectConfigurations: ConfigurableFileCollection

    /** Dokka Module Configuration files from other subprojects. */
    @get:InputFiles
//    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val dokkaModuleDescriptorFiles: ConfigurableFileCollection

    @get:LocalState
    abstract val cacheRoot: DirectoryProperty

    @get:Input
    abstract val delayTemplateSubstitution: Property<Boolean>

    @get:Nested
    abstract val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetGradleBuilder>

    @get:Input
    abstract val failOnWarning: Property<Boolean>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includes: ConfigurableFileCollection

    @get:Input
    abstract val finalizeCoroutines: Property<Boolean>

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    @get:Optional
    abstract val moduleVersion: Property<String>

    /**
     * The directory used by Dokka Generator as the output (not the output directory of this task).
     */
    @get:Internal // marked as Internal because this task does not use the directory contents, only the location
    val outputDir: DirectoryProperty = objects.directoryProperty()

    /**
     * Because [outputDir] must be [Internal] (so Gradle doesn't check the directory contents),
     * [outputDirPath] is required so Gradle can determine if the task is up-to-date.
     */
    @get:Input
    protected val outputDirPath: Provider<String> = outputDir.map { it.asFile.invariantSeparatorsPath }

    @get:Input
    abstract val offlineMode: Property<Boolean>

    @get:InputFiles
    @get:Classpath
    abstract val pluginsClasspath: ConfigurableFileCollection

    @get:Nested
    abstract val pluginsConfiguration: DomainObjectSet<DokkaPluginConfigurationGradleBuilder>

    @get:Input
    abstract val suppressObviousFunctions: Property<Boolean>

    @get:Input
    abstract val suppressInheritedMembers: Property<Boolean>

    /** @see org.jetbrains.dokka.gradle.dokka_configuration.DokkaPublication.enabled */
    @get:Input
//    @get:Optional
    abstract val publicationEnabled: Property<Boolean>

    init {
        description = "Assembles Dokka a configuration file, to be used when executing Dokka"
    }

    @TaskAction
    fun generateConfiguration() {
        val dokkaConfiguration = buildDokkaConfiguration()

        val encodedModuleDesc = jsonMapper.encodeToString(dokkaConfiguration)

        logger.info(encodedModuleDesc)

        dokkaConfigurationJson.get().asFile.writeText(encodedModuleDesc)
    }


    private fun buildDokkaConfiguration(): DokkaConfigurationKxs {

        val moduleName = moduleName.get()
        val moduleVersion = moduleVersion.orNull?.takeIf { it != "unspecified" }
        val outputDir = outputDir.asFile.get()
        val cacheRoot = cacheRoot.asFile.orNull
        val offlineMode = offlineMode.get()
        val sourceSets = dokkaSourceSets.filterNot {
            val suppressed = it.suppress.get()
            logger.info("Dokka source set ${it.sourceSetID.get()} ${if (suppressed) "is" else "isn't"} suppressed")
            suppressed
        }.map(DokkaSourceSetGradleBuilder::build)
        val pluginsClasspath = pluginsClasspath.files.toList()
        val pluginsConfiguration = pluginsConfiguration.map(DokkaPluginConfigurationGradleBuilder::build)
        val failOnWarning = failOnWarning.get()
        val delayTemplateSubstitution = delayTemplateSubstitution.get()
        val suppressObviousFunctions = suppressObviousFunctions.get()
        val includes = includes.files
        val suppressInheritedMembers = suppressInheritedMembers.get()
        val finalizeCoroutines = finalizeCoroutines.get()

        val dokkaModuleDescriptors = dokkaModuleDescriptors()


        // construct the base configuration for THIS project
        val baseDokkaConfiguration = DokkaConfigurationKxs(
            cacheRoot = cacheRoot,
            delayTemplateSubstitution = delayTemplateSubstitution,
            failOnWarning = failOnWarning,
            finalizeCoroutines = finalizeCoroutines,
            includes = includes,
            moduleName = moduleName,
            moduleVersion = moduleVersion,
            modulesKxs = dokkaModuleDescriptors,
            offlineMode = offlineMode,
            outputDir = outputDir,
            pluginsClasspath = pluginsClasspath,
            pluginsConfiguration = pluginsConfiguration,
            sourceSets = sourceSets,
            suppressInheritedMembers = suppressInheritedMembers,
            suppressObviousFunctions = suppressObviousFunctions,
        )

        // fetch any configurations from OTHER subprojects
        val subprojectConfigs = dokkaSubprojectConfigurations.files.map { file ->
            val fileContent = file.readText()
            jsonMapper.decodeFromString(DokkaConfigurationKxs.serializer(), fileContent)
        }

        // now, combine them:
        return subprojectConfigs.fold(baseDokkaConfiguration) { acc, it: DokkaConfigurationKxs ->
            acc.copy(
                sourceSets = acc.sourceSets + it.sourceSets,
                // TODO remove plugin classpath aggregation, plugin classpath should be shared via Gradle Configuration
                //      so Gradle can correctly de-duplicate jars
//                pluginsClasspath = acc.pluginsClasspath + it.pluginsClasspath,
            )
        }
    }

    private fun dokkaModuleDescriptors(): List<DokkaConfigurationKxs.DokkaModuleDescriptionKxs> =
        dokkaModuleDescriptorFiles.files.map { file ->
            val fileContent = file.readText()
            jsonMapper.decodeFromString(
                DokkaConfigurationKxs.DokkaModuleDescriptionKxs.serializer(),
                fileContent,
            )
        }

    @get:Input
    @Deprecated("TODO write adapter to the new DSL")
    abstract val pluginsMapConfiguration: MapProperty<String, String>
}
