package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationBuilder
import java.io.Serializable

abstract class DokkaPluginConfigurationGradleBuilder :
    DokkaConfigurationBuilder<DokkaConfigurationKxs.PluginConfigurationKxs>,
    Serializable {

    @get:Input
    abstract val fqPluginName: Property<String>

    @get:Input
    abstract val serializationFormat: Property<DokkaConfiguration.SerializationFormat>

    @get:Input
    abstract val values: Property<String>

    override fun build() = DokkaConfigurationKxs.PluginConfigurationKxs(
        fqPluginName = fqPluginName.get(),
        serializationFormat = serializationFormat.get(),
        values = values.get(),
    )

}
