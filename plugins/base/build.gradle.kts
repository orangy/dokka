import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    `java-test-fixtures`
    id("org.jetbrains.conventions.dokka-html-frontend-files")
}

dependencies {
    compileOnly(projects.core)

    implementation(kotlin("reflect"))

    implementation(libs.kotlinx.coroutines.core)

    compileOnly(projects.kotlinAnalysis)
    implementation(libs.jsoup)

    implementation(libs.jackson.kotlin)
    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }

    implementation(libs.freemarker)


    implementation(libs.kotlinx.html)

    testImplementation(testFixtures(projects.plugins.base))
    testImplementation(testFixtures(projects.core))

    testImplementation(projects.kotlinAnalysis)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    testFixturesImplementation(kotlin("reflect"))

    testFixturesCompileOnly(projects.plugins.base)
    testFixturesImplementation(testFixtures(projects.core))

    testFixturesImplementation(libs.jsoup)
    testFixturesImplementation(kotlin("test-junit"))

    dokkaHtmlFrontendFiles(projects.plugins.base.frontend) {
        because("fetch frontend files from subproject :plugins:base:frontend")
    }
}

// access the frontend files via the dependency on :plugins:base:frontend
val dokkaHtmlFrontendFiles: Provider<FileCollection> =
    configurations.dokkaHtmlFrontendFiles.map { frontendFiles ->
        frontendFiles.incoming.artifacts.artifactFiles
    }

val preparedokkaHtmlFrontendFiles by tasks.registering(Sync::class) {
    description = "copy Dokka Base frontend files into the resources directory"

    from(dokkaHtmlFrontendFiles) {
        include("*.js")
        into("dokka/scripts")
    }

    from(dokkaHtmlFrontendFiles) {
        include("*.css")
        into("dokka/styles")
    }

    into(layout.buildDirectory.dir("generated/src/main/resources"))
}

sourceSets.main {
    resources.srcDir(preparedokkaHtmlFrontendFiles.map { it.destinationDir })
}

tasks.test {
    maxHeapSize = "4G"
}

registerDokkaArtifactPublication("dokkaBase") {
    artifactId = "dokka-base"
}
