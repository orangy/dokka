package org.jetbrains.dokka.gradle.dokka_configuration

import org.gradle.api.Named
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.dokka.DokkaConfigurationBuilder
import org.jetbrains.dokka.DokkaSourceSetID

abstract class DokkaSourceSetIDGradleBuilder(
    private val named: String
) : DokkaConfigurationBuilder<DokkaSourceSetID>, Named {

    /**
     * Unique identifier of the scope that this source set is placed in.
     * Each scope provide only unique source set names.
     *
     * E.g. One DokkaTask inside the Gradle plugin represents one source set scope, since there cannot be multiple
     * source sets with the same name. However, a Gradle project will not be a proper scope, since there can be
     * multiple DokkaTasks that contain source sets with the same name (but different configuration)
     */
    @get:Input
    val scopeId: String get() = named

    @get:Input
    abstract var sourceSetName: String

    override fun build(): DokkaSourceSetID = DokkaSourceSetID(scopeId, sourceSetName)

    @Internal
    override fun getName(): String = named

}
