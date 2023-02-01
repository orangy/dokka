package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.build
import org.gradle.api.tasks.*

@CacheableTask
abstract class DokkaTask : AbstractDokkaLeafTask() {
    override fun buildDokkaConfiguration(): DokkaConfigurationImpl =
        DokkaConfigurationImpl(
            moduleName = moduleName.getSafe(),
            moduleVersion = moduleVersion.getValidVersionOrNull(),
            outputDir = outputDirectory.getSafe(),
            cacheRoot = cacheRoot.getSafe(),
            offlineMode = offlineMode.getSafe(),
            failOnWarning = failOnWarning.getSafe(),
            sourceSets = unsuppressedSourceSets.build(),
            pluginsConfiguration = buildPluginsConfiguration(),
            pluginsClasspath = plugins.resolve().toList(),
            suppressObviousFunctions = suppressObviousFunctions.getSafe(),
            suppressInheritedMembers = suppressInheritedMembers.getSafe(),
            finalizeCoroutines = finalizeCoroutines.get(),
        )
}
