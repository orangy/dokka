import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.kotlinAnalysis)

    implementation(kotlin("reflect"))
    implementation(libs.soywiz.korte)
    implementation(projects.plugins.base)
    implementation(projects.plugins.kotlinAsJava)

    implementation(libs.kotlinx.html)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(testFixtures(projects.plugins.base))
    testImplementation(testFixtures(projects.core))

    testImplementation(libs.jsoup)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("javadocPlugin") {
    artifactId = "javadoc-plugin"
}
