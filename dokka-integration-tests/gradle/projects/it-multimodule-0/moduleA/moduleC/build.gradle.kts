/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
    }
}
