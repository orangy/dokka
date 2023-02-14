plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.12.1")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
}
