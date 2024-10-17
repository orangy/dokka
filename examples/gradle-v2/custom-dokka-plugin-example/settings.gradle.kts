rootProject.name = "custom-dokka-plugin-example"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("/Users/dev/projects/jetbrains/dokka/dokka-runners/dokka-gradle-plugin/build/dev-maven-repo")
        maven("/Users/dev/projects/jetbrains/dokka/build/dev-maven-repo")
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("/Users/dev/projects/jetbrains/dokka/dokka-runners/dokka-gradle-plugin/build/dev-maven-repo")
        maven("/Users/dev/projects/jetbrains/dokka/build/dev-maven-repo")
    }
}

include(":dokka-plugin-hide-internal-api")
include(":demo-library")
