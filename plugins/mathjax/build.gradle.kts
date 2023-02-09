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

    val jsoup_version: String by project
    testImplementation("org.jsoup:jsoup:$jsoup_version")
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))
    testImplementation(kotlin("test-junit"))
    testImplementation(project(":kotlin-analysis"))

    testImplementation(project(":test-utils"))
    testImplementation(project(":core:test-api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}

registerDokkaArtifactPublication("mathjaxPlugin") {
    artifactId = "mathjax-plugin"
}
