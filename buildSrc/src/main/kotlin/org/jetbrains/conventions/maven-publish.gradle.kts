package org.jetbrains.conventions

plugins {
    id("org.jetbrains.conventions.base")
    `maven-publish`
    signing
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        repositories {
            // Publish to a project-local Maven directory, for verification. To test, run:
            // ./gradlew publishAllPublicationsToMavenProjectLocalRepository
            // and check $rootDir/build/maven-project-local
            maven(rootProject.layout.buildDirectory.dir("maven-project-local")) {
                name = "MavenProjectLocal"
            }
        }
    }
}
