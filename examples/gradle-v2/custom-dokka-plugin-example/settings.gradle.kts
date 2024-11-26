rootProject.name = "custom-dokka-plugin-example"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":dokka-plugin-hide-internal-api")
include(":demo-library")
