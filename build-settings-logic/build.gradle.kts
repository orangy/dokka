/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    `kotlin-dsl`
}

description = "Conventions for use in settings.gradle.kts scripts"

kotlin {
    jvmToolchain {
        languageVersion = libs.versions.gradleDaemonJvm.map(String::toInt).map(JavaLanguageVersion::of)
    }
}

dependencies {
    implementation(libs.gradlePlugin.gradle.develocity)
    implementation(libs.gradlePlugin.gradle.customUserData)
    implementation(libs.gradlePlugin.gradle.foojayToolchains)
}
