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
    implementation(project(":plugins:jekyll"))
    implementation(project(":plugins:all-modules-page"))
    implementation(project(":plugins:templating"))
    implementation(project(":plugins:gfm"))
    implementation(project(":plugins:gfm:gfm-template-processing"))

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    testImplementation(project(":test-utils"))
    testImplementation(project(":core:test-api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}

registerDokkaArtifactPublication("dokkaJekyllTemplateProcessing") {
    artifactId = "jekyll-template-processing-plugin"
}
