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

    compileOnly(project(":plugins:base"))
    implementation(project(":core:test-api"))
    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")
    implementation(kotlin("test-junit"))

    testImplementation(project(":test-utils"))
    testImplementation(project(":core:test-api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}

registerDokkaArtifactPublication("dokkaBaseTestUtils") {
    artifactId = "dokka-base-test-utils"
}
