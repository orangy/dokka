rootProject.name = "buildSrc"

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
