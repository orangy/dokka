rootProject.name = "dokka-gradle-plugin-2"

@Suppress("UnstableApiUsage") // centralised repositories are incubating
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
