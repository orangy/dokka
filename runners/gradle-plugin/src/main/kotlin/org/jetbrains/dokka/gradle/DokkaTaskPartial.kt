package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.build
import org.gradle.api.tasks.*

@CacheableTask
abstract class DokkaTaskPartial : AbstractDokkaLeafTask() {

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            moduleName = moduleName.getSafe(),
            moduleVersion = moduleVersion.orNull,
            outputDir = outputDirectory.getSafe(),
            cacheRoot = cacheRoot.getSafe(),
            offlineMode = offlineMode.getSafe(),
            failOnWarning = failOnWarning.getSafe(),
            sourceSets = unsuppressedSourceSets.build(),
            pluginsConfiguration = buildPluginsConfiguration(),
            pluginsClasspath = plugins.resolve().toList(),
            delayTemplateSubstitution = true,
            suppressObviousFunctions = suppressObviousFunctions.getSafe(),
            suppressInheritedMembers = suppressInheritedMembers.getSafe(),
            finalizeCoroutines = finalizeCoroutines.get(),
        )
    }
}
