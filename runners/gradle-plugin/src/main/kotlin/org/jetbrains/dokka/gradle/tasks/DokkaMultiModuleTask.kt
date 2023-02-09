package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.internal.tasks.TaskDependencyInternal
import org.gradle.api.provider.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaModuleDescriptionImpl
import org.jetbrains.dokka.gradle.DokkaMultiModuleFileLayout
import org.jetbrains.dokka.gradle.tasks.*
import org.jetbrains.dokka.gradle.*
import java.io.File

@Suppress("unused") // Shall provide source compatibility if possible
@Deprecated("Use 'DokkaMultimoduleTask' instead", ReplaceWith("DokkaMultimoduleTask"))
typealias DokkaMultimoduleTask = DokkaMultiModuleTask

private typealias TaskPath = String

@CacheableTask
abstract class DokkaMultiModuleTask : AbstractDokkaParentTask() {

    /**
     * List of Markdown files that contain
     * [module and package documentation](https://kotlinlang.org/docs/reference/dokka-module-and-package-docs.html).
     *
     * Contents of specified files will be parsed and embedded into documentation as module and package descriptions.
     *
     * Example of such a file:
     *
     * ```markdown
     * # Module kotlin-demo
     *
     * The module shows the Dokka usage.
     *
     * # Package org.jetbrains.kotlin.demo
     *
     * Contains assorted useful stuff.
     *
     * ## Level 2 heading
     *
     * Text after this heading is also part of documentation for `org.jetbrains.kotlin.demo`
     *
     * # Package org.jetbrains.kotlin.demo2
     *
     * Useful stuff in another package.
     * ```
     */
    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    val includes: ConfigurableFileCollection = project.files()

    @Internal
    val fileLayout: Property<DokkaMultiModuleFileLayout> = project.objects.property<DokkaMultiModuleFileLayout>()
        .convention(DokkaMultiModuleFileLayout.CompactInParent)

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val sourceChildOutputDirectories: Iterable<File>
        get() = childDokkaTasks.map { task -> task.outputDirectory.get() }

    @get:OutputDirectories
    internal val targetChildOutputDirectories: Iterable<File>
        get() = childDokkaTasks.map { task -> targetChildOutputDirectory(task) }

    @get:Input
    internal val childDokkaTaskIncludes: Map<TaskPath, Set<File>>
        get() = childDokkaTasks.filterIsInstance<DokkaTaskPartial>().associate { task ->
            task.path to task.dokkaSourceSets.flatMap { it.includes }.toSet()
        }

    @Internal
    override fun getTaskDependencies(): TaskDependencyInternal =
        super.getTaskDependencies() + childDokkaTasks


    override fun generateDocumentation() {
        checkChildDokkaTasksIsNotEmpty()
        super.generateDocumentation()
    }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl = DokkaConfigurationImpl(
        moduleName = moduleName.get(),
        moduleVersion = moduleVersion.getValidVersionOrNull(),
        outputDir = outputDirectory.get(),
        cacheRoot = cacheRoot.orNull,
        pluginsConfiguration = buildPluginsConfiguration(),
        failOnWarning = failOnWarning.get(),
        offlineMode = offlineMode.get(),
        pluginsClasspath = plugins.resolve().toList(),
        modules = childDokkaTasks.map { dokkaTask ->
            DokkaModuleDescriptionImpl(
                name = dokkaTask.moduleName.get(),
                relativePathToOutputDirectory = targetChildOutputDirectory(dokkaTask).relativeTo(outputDirectory.get()),
                includes = childDokkaTaskIncludes[dokkaTask.path].orEmpty(),
                sourceOutputDirectory = dokkaTask.outputDirectory.get()
            )
        },
        includes = includes.toSet(),
        finalizeCoroutines = finalizeCoroutines.get(),
    )
}
