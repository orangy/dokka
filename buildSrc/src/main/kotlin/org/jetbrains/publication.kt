package org.jetbrains

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.DokkaPublicationChannel.*
import java.net.URI

class DokkaPublicationBuilder {
    enum class Component {
        Java, Shadow
    }

    var artifactId: String? = null
    var component: Component = Component.Java
}


fun Project.registerDokkaArtifactPublication(publicationName: String, configure: DokkaPublicationBuilder.() -> Unit) {
    configure<PublishingExtension> {
        publications {
            register<MavenPublication>(publicationName) {
                val builder = DokkaPublicationBuilder().apply(configure)
                artifactId = builder.artifactId
                when (builder.component) {
                    DokkaPublicationBuilder.Component.Java -> from(components["java"])
                    DokkaPublicationBuilder.Component.Shadow -> run {
                        extensions.getByType(ShadowExtension::class.java).component(this)
                        artifact(tasks["sourcesJar"])
                    }
                }
                artifact(tasks["javadocJar"])
                configurePom("Dokka ${project.name}")
            }
        }
    }

    configureSpacePublicationIfNecessary(publicationName)
    configureSonatypePublicationIfNecessary(publicationName)
    createDokkaPublishTaskIfNecessary()
}

fun Project.configureSpacePublicationIfNecessary(vararg publications: String) {
    if (SPACE_DOKKA_DEV in this.publicationChannels) {
        configure<PublishingExtension> {
            repositories {
                /* already registered */
                findByName(SPACE_DOKKA_DEV.name)?.let { return@repositories }
                maven {
                    name = SPACE_DOKKA_DEV.name
                    url = URI.create("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
                    credentials {
                        username = System.getenv("SPACE_PACKAGES_USER")
                        password = System.getenv("SPACE_PACKAGES_SECRET")
                    }
                }
            }
        }
    }

    whenEvaluated {
        tasks.withType<PublishToMavenRepository> {
            if (this.repository.name == SPACE_DOKKA_DEV.name) {
                this.isEnabled = this.isEnabled && publication.name in publications
                if (!this.isEnabled) {
                    this.group = "disabled"
                }
            }
        }
    }
}

fun Project.createDokkaPublishTaskIfNecessary() {
    tasks.maybeCreate("dokkaPublish").run {
        if (publicationChannels.any { it.isSpaceRepository() }) {
            dependsOn(tasks.named("publish"))
        }

        if (publicationChannels.any { it.isMavenRepository() }) {
            dependsOn(tasks.named("publishToSonatype"))
        }

        if (publicationChannels.any { it.isGradlePluginPortal() }) {
            dependsOn(tasks.named("publishPlugins"))
        }
    }
}

fun Project.configureSonatypePublicationIfNecessary(vararg publications: String) {
    if (publicationChannels.any { it.isMavenRepository() }) {
        signPublicationsIfKeyPresent(*publications)
    }
}

fun MavenPublication.configurePom(projectName: String) {
    pom {
        name.set(projectName)
        description.set("Dokka is an API documentation engine for Kotlin and Java, performing the same function as Javadoc for Java")
        url.set("https://github.com/Kotlin/dokka")

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("JetBrains")
                name.set("JetBrains Team")
                organization.set("JetBrains")
                organizationUrl.set("http://www.jetbrains.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/Kotlin/dokka.git")
            url.set("https://github.com/Kotlin/dokka/tree/master")
        }
    }
}

@Suppress("UnstableApiUsage")
private fun Project.signPublicationsIfKeyPresent(vararg publications: String) {
    val signingKeyId: String? = System.getenv("SIGN_KEY_ID")
    val signingKey: String? = System.getenv("SIGN_KEY")
    val signingKeyPassphrase: String? = System.getenv("SIGN_KEY_PASSPHRASE")

    if (!signingKey.isNullOrBlank()) {
        extensions.configure<SigningExtension>("signing") {
            if (signingKeyId?.isNotBlank() == true) {
                useInMemoryPgpKeys(signingKeyId, signingKey, signingKeyPassphrase)
            } else {
                useInMemoryPgpKeys(signingKey, signingKeyPassphrase)
            }
            publications.forEach { publicationName ->
                extensions.findByType(PublishingExtension::class)!!.publications.findByName(publicationName)?.let {
                    sign(it)
                }
            }
        }
    }
}
