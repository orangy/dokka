/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package buildsrc.conventions

import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.`java-base`

plugins {
    id("buildsrc.conventions.base")
    `java`
}

extensions.getByType<JavaPluginExtension>().apply {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withSourcesJar()
}
