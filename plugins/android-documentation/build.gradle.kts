import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
}

dependencies {
    compileOnly(project(":core"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation(project(":plugins:base"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":test-utils"))
    testImplementation(project(":core:test-api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}

registerDokkaArtifactPublication("androidDocumentationPlugin") {
    artifactId = "android-documentation-plugin"
}
