plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    // Import Gradle Plugins that will be used in the buildSrc pre-compiled script plugins, and any `build.gradle.kts`
    // files in the project.
    // Use their Maven coordinates (plus versions), not Gradle plugin IDs!
    // This should be the only place that Gradle plugin versions are defined, so they are aligned across all build scripts

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    implementation("com.github.jengelman.gradle.plugins:shadow:2.0.4")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.12.1")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.10")
    implementation("com.gradle.publish:plugin-publish-plugin:0.20.0")
}
