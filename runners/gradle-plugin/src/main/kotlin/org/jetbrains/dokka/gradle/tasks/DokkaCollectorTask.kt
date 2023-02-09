package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.tasks.CacheableTask
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.gradle.checkChildDokkaTasksIsNotEmpty

@CacheableTask
abstract class DokkaCollectorTask : AbstractDokkaParentTask() {

    override fun generateDocumentation() {
        checkChildDokkaTasksIsNotEmpty()
        super.generateDocumentation()
    }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        val initialDokkaConfiguration = DokkaConfigurationImpl(
            moduleName = moduleName.get(),
            outputDir = outputDirectory.get(),
            cacheRoot = cacheRoot.orNull,
            failOnWarning = failOnWarning.get(),
            offlineMode = offlineMode.get(),
            pluginsClasspath = plugins.resolve().toList(),
            pluginsConfiguration = buildPluginsConfiguration(),
            suppressObviousFunctions = suppressObviousFunctions.get(),
            suppressInheritedMembers = suppressInheritedMembers.get(),
            finalizeCoroutines = finalizeCoroutines.get(),
        )

        val subprojectDokkaConfigurations = childDokkaTasks.map { dokkaTask -> dokkaTask.buildDokkaConfiguration() }
        return subprojectDokkaConfigurations.fold(initialDokkaConfiguration) { acc, it: DokkaConfigurationImpl ->
            acc.copy(
                sourceSets = acc.sourceSets + it.sourceSets,
                pluginsClasspath = acc.pluginsClasspath + it.pluginsClasspath
            )
        }
    }
}
