import org.jetbrains.ValidatePublications
import org.jetbrains.publicationChannels

plugins {
    org.jetbrains.conventions.base
    id("org.jetbrains.dokka")
    id("io.github.gradle-nexus.publish-plugin")

    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

val dokka_version: String by project

group = "org.jetbrains.dokka"
version = dokka_version


logger.lifecycle("Publication version: $dokka_version")
tasks.register<ValidatePublications>("validatePublications")

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USER"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

val dokkaPublish by tasks.registering {
    if (publicationChannels.any { it.isMavenRepository() }) {
        finalizedBy(tasks.named("closeAndReleaseSonatypeStagingRepository"))
    }
}

apiValidation {
    ignoredProjects.addAll(
        setOf(
            "search-component",
            "compiler-dependency",
            "kotlin-analysis",
            "intellij-dependency",
            "frontend",

            "integration-tests",
            "gradle",
            "cli",
            "maven",

            "test-utils",
        )
    )
}
