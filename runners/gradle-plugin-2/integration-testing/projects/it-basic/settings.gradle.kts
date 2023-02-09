rootProject.name = "it-basic"

@Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        providers.gradleProperty("testMavenRepoDir").orNull?.let { maven(it) }
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        providers.gradleProperty("testMavenRepoDir").orNull?.let { maven(it) }
    }
}
