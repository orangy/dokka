/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import kotlinx.serialization.json.*
import kotlin.test.*

class DokkaModuleDescriptionKxsTest {

    @Test
    fun testToJsonObject() {
        val module = DokkaModuleDescriptionKxs(
            name = "moduleName",
            modulePath = "/path/to/module",
            moduleOutputDirName = "outputDir",
            moduleIncludesDirName = "includesDir"
        )
        val jsonObject = DokkaModuleDescriptionKxs.toJsonObject(module)

        assertString("moduleName", jsonObject["name"])
        assertString("/path/to/module", jsonObject["modulePath"])
        assertString("outputDir", jsonObject["moduleOutputDirName"])
        assertString("includesDir", jsonObject["moduleIncludesDirName"])
    }

    @Test
    fun testFromJsonObject() {
        val jsonObject = buildJsonObject {
            put("name", "moduleName")
            put("modulePath", "/path/to/module")
            put("moduleOutputDirName", "outputDir")
            put("moduleIncludesDirName", "includesDir")
        }
        val module = DokkaModuleDescriptionKxs.fromJsonObject(jsonObject)

        assertEquals("moduleName", module.name)
        assertEquals("/path/to/module", module.modulePath)
        assertEquals("outputDir", module.moduleOutputDirName)
        assertEquals("includesDir", module.moduleIncludesDirName)
    }

    @Test
    fun testDefaultConstructorArguments() {
        val jsonObject = buildJsonObject {
            put("name", "defaultModule")
            put("modulePath", "/default/path")
        }
        val module = DokkaModuleDescriptionKxs.fromJsonObject(jsonObject)

        assertEquals("defaultModule", module.name)
        assertEquals("/default/path", module.modulePath)

        // those are default values coming from DokkaModuleDescriptionKxs Companion
        assertEquals("module", module.moduleOutputDirName)
        assertEquals("includes", module.moduleIncludesDirName)
    }

    private fun assertString(expected: String, actual: JsonElement?) {
        assertNotNull(actual)
        assertIs<JsonPrimitive>(actual)
        assertTrue(actual.isString)
        assertEquals(expected, actual.content)
    }
}
