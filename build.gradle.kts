import org.jetbrains.ValidatePublications
import org.jetbrains.publicationChannels

@Suppress("DSL_SCOPE_VIOLATION") // fixed in Gradle 8.1 https://github.com/gradle/gradle/pull/23639
plugins {
    id("org.jetbrains.conventions.base")
    id("org.jetbrains.conventions.dokka")

    alias(libs.plugins.gradlePublish)
    alias(libs.plugins.nexusPublish)
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
