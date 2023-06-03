import org.jetbrains.dokkaVersion
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    `java-test-fixtures`
}

dependencies {
    api(libs.jetbrains.markdown)
    implementation(kotlin("reflect"))

    implementation(libs.jsoup)

    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.xml)
    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test-junit"))

    testFixturesImplementation(kotlin("reflect"))
    testFixturesImplementation(libs.assertk)

    testFixturesApi(projects.core)
    testFixturesImplementation(projects.kotlinAnalysis)
    testFixturesImplementation("junit:junit:4.13.2") // TODO: remove dependency to junit
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
