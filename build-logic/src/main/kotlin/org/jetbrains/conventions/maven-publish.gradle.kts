package org.jetbrains.conventions

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import org.gradle.internal.component.external.model.TestFixturesSupport.TEST_FIXTURE_SOURCESET_NAME

plugins {
    id("org.jetbrains.conventions.base")
    `maven-publish`
    signing
    id("org.jetbrains.conventions.dokka")
    id("dev.adamko.kotlin.binary-compatibility-validator")
}

val javadocJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles a Javadoc JAR using Dokka HTML"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

publishing {
    repositories {
        // Publish to a project-local Maven directory, for verification. To test, run:
        // ./gradlew publishAllPublicationsToMavenProjectLocalRepository
        // and check $rootDir/build/maven-project-local
        maven(rootProject.layout.buildDirectory.dir("maven-project-local")) {
            name = "MavenProjectLocal"
        }
    }

    publications.withType<MavenPublication>().configureEach {
        artifact(javadocJar)

        pom {
            name.convention(provider { "Dokka ${project.name}" })
            description.convention("Dokka is an API documentation engine for Kotlin and Java, performing the same function as Javadoc for Java")
            url.convention("https://github.com/Kotlin/dokka")

            licenses {
                license {
                    name.convention("The Apache Software License, Version 2.0")
                    url.convention("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.convention("repo")
                }
            }

            developers {
                developer {
                    id.convention("JetBrains")
                    name.convention("JetBrains Team")
                    organization.convention("JetBrains")
                    organizationUrl.convention("https://www.jetbrains.com")
                }
            }

            scm {
                connection.convention("scm:git:git://github.com/Kotlin/dokka.git")
                url.convention("https://github.com/Kotlin/dokka/tree/master")
            }
        }
    }
}

plugins.withType<ShadowPlugin>().configureEach {
    // manually disable publication of Shadow elements https://github.com/johnrengelman/shadow/issues/651#issue-839148311
    // This is done to preserve compatibility and have the same behaviour as previous versions of Dokka.
    // For more details, see https://github.com/Kotlin/dokka/pull/2704#issuecomment-1499517930
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
}

binaryCompatibilityValidator {
    targets.matching { it.name == TEST_FIXTURE_SOURCESET_NAME }.configureEach {
        enabled.set(true)
    }
}
