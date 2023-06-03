import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    implementation(kotlin("reflect"))
    implementation(projects.plugins.base)
    implementation(projects.plugins.gfm)

    testImplementation(testFixtures(projects.core))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("jekyllPlugin") {
    artifactId = "jekyll-plugin"
}
