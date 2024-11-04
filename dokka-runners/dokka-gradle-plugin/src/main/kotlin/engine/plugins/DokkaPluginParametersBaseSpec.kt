/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.plugins

import org.gradle.api.Named
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import java.io.Serializable
import javax.inject.Inject

/**
 * Base class for defining Dokka Plugin configuration.
 *
 * This class should not be instantiated directly.
 * Instead, define a subclass that implements the [jsonEncode] function.
 *
 * @param[name] A descriptive name of the item in the [org.jetbrains.dokka.gradle.internal.DokkaPluginParametersContainer].
 * The name is only used for identification in the Gradle buildscripts.
 * @param[pluginFqn] Fully qualified classname of the Dokka Plugin
 */
abstract class DokkaPluginParametersBaseSpec
@InternalDokkaGradlePluginApi
@Inject
constructor(
    private val name: String,
    @get:Input
    open val pluginFqn: String,
) : Serializable, Named {

    /**
     * Must be implemented by subclasses.
     *
     * Returns JSON encoded configuration, to be parsed by the Dokka plugin identified by [pluginFqn].
     */
    abstract fun jsonEncode(): String // to be implemented by subclasses

    @Input
    override fun getName(): String = name
}
