import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.4")
    implementation(project(":core"))
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-cli-$dokka_version.jar")
        archiveClassifier.set("")
        manifest {
            attributes("Main-Class" to "org.jetbrains.dokka.MainKt")
        }
    }
}

registerDokkaArtifactPublication("dokkaCli") {
    artifactId = "dokka-cli"
    component = Shadow
}
