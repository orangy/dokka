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
    implementation(project(":plugins:gfm"))

    testImplementation(project(":test-utils"))
    testImplementation(project(":core:test-api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}

registerDokkaArtifactPublication("jekyllPlugin") {
    artifactId = "jekyll-plugin"
}
