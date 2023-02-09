package org.jetbrains.dokka.gradle.tasks

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.build
import org.gradle.api.tasks.*

@CacheableTask
abstract class DokkaTaskPartial : AbstractDokkaLeafTask() {

    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        return DokkaConfigurationImpl(
            moduleName = moduleName.get(),
            moduleVersion = moduleVersion.orNull,
            outputDir = outputDirectory.get(),
            cacheRoot = cacheRoot.orNull,
            offlineMode = offlineMode.get(),
            failOnWarning = failOnWarning.get(),
            sourceSets = unsuppressedSourceSets.build(),
            pluginsConfiguration = buildPluginsConfiguration(),
            pluginsClasspath = plugins.resolve().toList(),
            delayTemplateSubstitution = true,
            suppressObviousFunctions = suppressObviousFunctions.get(),
            suppressInheritedMembers = suppressInheritedMembers.get(),
            finalizeCoroutines = finalizeCoroutines.get(),
        )
    }
}
