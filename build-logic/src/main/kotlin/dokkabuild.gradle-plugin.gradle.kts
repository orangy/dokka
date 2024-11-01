/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.utils.configureGradleKotlinCompatibility
import dokkabuild.utils.excludeGradleEmbeddedDependencies

plugins {
    id("org.gradle.kotlin.kotlin-dsl")
    id("dokkabuild.java")
    id("dokkabuild.publish-gradle-plugin")
}

configureGradleKotlinCompatibility()

tasks.compileKotlin {
    compilerOptions {
        // `kotlin-dsl` plugin overrides the versions at the task level,
        // which takes priority over the `kotlin` project extension.
        // So, fix it by manually setting the LV per-task.
        languageVersion.set(kotlin.compilerOptions.languageVersion)
        apiVersion.set(kotlin.compilerOptions.apiVersion)
    }
}

tasks.validatePlugins {
    enableStricterValidation = true
}

excludeGradleEmbeddedDependencies(sourceSets.main)
