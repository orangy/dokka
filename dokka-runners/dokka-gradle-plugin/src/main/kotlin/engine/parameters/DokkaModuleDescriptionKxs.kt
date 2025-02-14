/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.gradle.kotlin.dsl.java
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi

/**
 * Any subproject can be merged into a single Dokka Publication. To do this, first it must create
 * a Dokka Module. A [DokkaModuleDescriptionKxs] describes a config file for the Dokka Module that
 * describes its content. This config file will be used by any aggregating project to produce
 * a Dokka Publication with multiple modules.
 *
 * Note: this class implements [java.io.Serializable] because it is used as a
 * [Gradle Property][org.gradle.api.provider.Property], and Gradle must be able to fingerprint
 * property values classes using Java Serialization.
 *
 * All other configuration data classes also implement [java.io.Serializable] via their parent interfaces.
 *
 * @see org.jetbrains.dokka.gradle.engine.parameters.DokkaModuleDescriptionKxs
 * @see org.jetbrains.dokka.DokkaModuleDescriptionImpl
 */
@InternalDokkaGradlePluginApi
data class DokkaModuleDescriptionKxs(
    /** @see DokkaConfiguration.DokkaModuleDescription.name */
    val name: String,
    /** @see org.jetbrains.dokka.gradle.DokkaExtension.modulePath */
    val modulePath: String,
    /** name of the sibling directory that contains the module output */
    val moduleOutputDirName: String = defaultModuleOutputDirName,
    /** name of the sibling directory that contains the module includes */
    val moduleIncludesDirName: String = defaultModuleIncludesDirName,
) {
    internal companion object {
        private val defaultModuleOutputDirName = "module"
        private val defaultModuleIncludesDirName = "includes"

        fun toJsonObject(module: DokkaModuleDescriptionKxs): JsonObject = buildJsonObject {
            put("name", module.name)
            put("modulePath", module.modulePath)
            put("moduleOutputDirName", module.moduleOutputDirName)
            put("moduleIncludesDirName", module.moduleIncludesDirName)
        }

        fun fromJsonObject(obj: JsonObject): DokkaModuleDescriptionKxs = DokkaModuleDescriptionKxs(
            name = obj.getString("name"),
            modulePath = obj.getString("modulePath"),
            moduleOutputDirName = obj.getString("moduleOutputDirName", defaultModuleOutputDirName),
            moduleIncludesDirName = obj.getString("moduleIncludesDirName", defaultModuleIncludesDirName),
        )

        private fun JsonObject.getString(key: String, defaultValue: String? = null): String {
            return get(key)?.jsonPrimitive?.content ?: defaultValue ?: error("Missing required property '$key'")
        }
    }
}
