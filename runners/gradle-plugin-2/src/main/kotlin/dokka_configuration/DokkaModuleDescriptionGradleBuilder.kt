package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaConfigurationBuilder
import javax.inject.Inject

abstract class DokkaModuleDescriptionGradleBuilder @Inject constructor(
    providers: ProviderFactory,
) : DokkaConfigurationBuilder<DokkaConfigurationKxs.DokkaModuleDescriptionKxs>, Named {

    @get:Input
    val moduleName: Provider<String> = providers.provider { name }

    @get:Input
    abstract val sourceOutputDirectory: RegularFileProperty

    @get:Input
    abstract val includes: ConfigurableFileCollection

    @Internal
    override fun build() =
        DokkaConfigurationKxs.DokkaModuleDescriptionKxs(
            moduleName = moduleName.get(),
            sourceOutputDirectory = sourceOutputDirectory.get().asFile,
            includes = includes.files,
        )
}
