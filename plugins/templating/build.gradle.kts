import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
}

registerDokkaArtifactPublication("templating-plugin") {
    artifactId = "templating-plugin"
}

dependencies {
    compileOnly(project(":core"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(project(":plugins:base"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    val jackson_version: String by project
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    val jackson_databind_version: String by project
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_databind_version") {
            because("CVE-2022-42003")
        }
    }
    val kotlinx_html_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")

    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")
    testImplementation(project(":plugins:base:base-test-utils"))

    testImplementation(project(":test-utils"))
    testImplementation(project(":core:test-api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}
