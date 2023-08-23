import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("org.jetbrains.conventions.base-unit-test")
}

dependencies {
    compileOnly(projects.core)

    implementation(projects.plugins.base)

    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.jsoup)
    testImplementation(projects.core.contentMatcherTestUtils)
    testImplementation(projects.core.testApi)

    symbolsTestConfiguration(project(path = ":subprojects:analysis-kotlin-symbols", configuration = "shadow"))
    descriptorsTestConfiguration(project(path = ":subprojects:analysis-kotlin-descriptors", configuration = "shadow"))
    testImplementation(projects.plugins.base.baseTestUtils) {
        exclude(module = "analysis-kotlin-descriptors")
    }
}

registerDokkaArtifactPublication("mathjaxPlugin") {
    artifactId = "mathjax-plugin"
}
