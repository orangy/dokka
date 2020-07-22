import org.jetbrains.registerDokkaArtifactPublication

dependencies {
    testImplementation("org.jsoup:jsoup:1.12.1")
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:javadoc"))
    testImplementation(project(":plugins:base:test-utils"))
    testImplementation(project(":test-tools"))
    testImplementation(kotlin("test-junit"))
    testImplementation(project(":kotlin-analysis"))
}

registerDokkaArtifactPublication("mathjaxPlugin") {
    artifactId = "mathjax-plugin"
}
