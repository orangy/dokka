package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.tasks.CacheableTask
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.gradle.checkChildDokkaTasksIsNotEmpty
import org.jetbrains.dokka.gradle.getSafe

@CacheableTask
abstract class DokkaCollectorTask : AbstractDokkaParentTask() {

    override fun generateDocumentation() {
        checkChildDokkaTasksIsNotEmpty()
        super.generateDocumentation()
    }

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        val initialDokkaConfiguration = DokkaConfigurationImpl(
            moduleName = moduleName.getSafe(),
            outputDir = outputDirectory.asFile.get(),
            cacheRoot = cacheRoot.asFile.orNull,
            failOnWarning = failOnWarning.getSafe(),
            offlineMode = offlineMode.getSafe(),
            pluginsClasspath = plugins.resolve().toList(),
            pluginsConfiguration = buildPluginsConfiguration(),
            suppressObviousFunctions = suppressObviousFunctions.getSafe(),
            suppressInheritedMembers = suppressInheritedMembers.getSafe(),
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
