package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.Serializable

abstract class DokkaConfigurationGradleBuilder :
//    DokkaConfigurationBuilder<DokkaConfigurationKxs>,
    Serializable {

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    @get:Optional
    abstract val moduleVersion: Property<String>

    @get:Internal
    // marked as Internal because this task does not use the directory contents, only the location
    abstract val outputDir: DirectoryProperty

    /**
     * Because [outputDir] must be [Internal] (so Gradle doesn't check the directory contents),
     * [outputDirPath] is required so Gradle can determine if the task is up-to-date.
     */
    @get:Input
    protected val outputDirPath: Provider<String>
        get() = outputDir.map { it.asFile.invariantSeparatorsPath }

    @get:Internal
    // marked as Internal because this task does not use the directory contents, only the location
    abstract val cacheRoot: DirectoryProperty

    /**
     * Because [cacheRoot] must be [Internal] (so Gradle doesn't check the directory contents),
     * [cacheRootPath] is required so Gradle can determine if the task is up-to-date.
     */
    @get:Input
    @get:Optional
    protected val cacheRootPath: Provider<String>
        get() = cacheRoot.map { it.asFile.invariantSeparatorsPath }

    @get:Input
    abstract val offlineMode: Property<Boolean>

    /**
     * Dokka Source Sets describe the source code that should be included in a Dokka Publication.
     *
     * Dokka will not generate documentation unless there is at least there is at least one Dokka Source Set.
     *
     * Only source sets that are contained within _this project_ should be included here.
     * To merge source sets from other projects, use the Gradle dependencies block.
     *
     * ```kotlin
     * dependencies {
     *   // merge :other-project into this project's Dokka Configuration
     *   dokka(project(":other-project"))
     * }
     * ```
     *
     * Or, to include other Dokka Publications as a Dokka Module use
     *
     * ```kotlin
     * dependencies {
     *   // include :other-project as a module in this project's Dokka Configuration
     *   dokkaModule(project(":other-project"))
     * }
     * ```
     *
     * Dokka will merge Dokka Source Sets from other subprojects.
     */
    @get:Nested
    abstract val dokkaSourceSets: NamedDomainObjectContainer<DokkaSourceSetGradleBuilder>

//    @get:InputFiles
//    @get:Classpath
//    abstract val pluginsClasspath: ConfigurableFileCollection

    @get:Nested
    abstract val pluginsConfiguration: NamedDomainObjectContainer<DokkaPluginConfigurationGradleBuilder>

//    /** Dokka Configuration files from other subprojects that will be merged into this Dokka Configuration */
//    @get:InputFiles
////    @get:NormalizeLineEndings
//    @get:PathSensitive(PathSensitivity.NAME_ONLY)
//    abstract val dokkaSubprojectConfigurations: ConfigurableFileCollection

//    /** Dokka Module Configuration from other subprojects. */
//    @get:InputFiles
////    @get:NormalizeLineEndings
//    @get:PathSensitive(PathSensitivity.NAME_ONLY)
//    abstract val dokkaModuleDescriptorFiles: ConfigurableFileCollection

    @get:Input
    abstract val failOnWarning: Property<Boolean>

    @get:Input
    abstract val delayTemplateSubstitution: Property<Boolean>

    @get:Input
    abstract val suppressObviousFunctions: Property<Boolean>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val includes: ConfigurableFileCollection

    @get:Input
    abstract val suppressInheritedMembers: Property<Boolean>

    @get:Input
    // TODO probably not needed any more, since Dokka Generator now runs in an isolated JVM process
    abstract val finalizeCoroutines: Property<Boolean>

    /**
     * Dokka Module Descriptions describe an independent Dokka publication, and these
     * descriptions are used by _other_ Dokka Configurations.
     *
     * The other Dokka Modules must have been generated using [delayTemplateSubstitution] set to `true`.
     *
     * Only add a module if you want the Dokka Publication produced by _this project_ to be
     * included in the Dokka Publication of _another_ project.
     */
    abstract val dokkaModules: NamedDomainObjectContainer<DokkaModuleDescriptionGradleBuilder>

//    override fun build() =
//        DokkaConfigurationKxs(
//            moduleName = moduleName.get(),
//            moduleVersion = moduleVersion.get(),
//            outputDir = outputDir.asFile.get(),
//            cacheRoot = cacheRoot.asFile.orNull,
//            offlineMode = offlineMode.get(),
//            failOnWarning = failOnWarning.get(),
//            sourceSets = dokkaSourceSets.map(DokkaSourceSetGradleBuilder::build),
//            pluginsClasspath = pluginsClasspath.files.toList(),
//            pluginsConfiguration = pluginsConfiguration.map { it.build() },
//            delayTemplateSubstitution = delayTemplateSubstitution.get(),
//            suppressObviousFunctions = suppressObviousFunctions.get(),
//            includes = includes.files,
//            suppressInheritedMembers = suppressInheritedMembers.get(),
//            finalizeCoroutines = finalizeCoroutines.get(),
//            modulesKxs = dokkaModules.map(DokkaModuleDescriptionGradleBuilder::build),
//        )
}
