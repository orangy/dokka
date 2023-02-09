import org.jetbrains.dokkaVersion
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
}

dependencies {
    api("org.jetbrains:markdown:0.3.1")
    implementation(kotlin("reflect"))

    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")

    val jackson_version: String by project
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jackson_version")
    val jackson_databind_version: String by project
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_databind_version") {
            because("CVE-2022-42003")
        }
    }

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    testImplementation(project(":core:test-api"))
    testImplementation(kotlin("test-junit"))
}

tasks {
    processResources {
        inputs.property("dokkaVersion", dokkaVersion)
        eachFile {
            if (name == "dokka-version.properties") {
                filter { line ->
                    line.replace("<dokka-version>", dokkaVersion)
                }
            }
        }
    }
}

registerDokkaArtifactPublication("dokkaCore") {
    artifactId = "dokka-core"
}
