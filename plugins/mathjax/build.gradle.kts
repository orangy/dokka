import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    implementation(kotlin("reflect"))
    implementation(projects.plugins.base)

    testImplementation(testFixtures(projects.plugins.base))
    testImplementation(testFixtures(projects.core))

    testImplementation(libs.jsoup)
    testImplementation(kotlin("test-junit"))
    testImplementation(projects.kotlinAnalysis)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("mathjaxPlugin") {
    artifactId = "mathjax-plugin"
}
