import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    implementation(kotlin("reflect"))
    compileOnly(projects.kotlinAnalysis)
    implementation(projects.plugins.base)

    testImplementation(testFixtures(projects.plugins.base))
    testImplementation(testFixtures(projects.core))

    testImplementation(projects.plugins.base)
    testImplementation(libs.jsoup)
    testImplementation(projects.kotlinAnalysis)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("kotlinAsJavaPlugin") {
    artifactId = "kotlin-as-java-plugin"
}
