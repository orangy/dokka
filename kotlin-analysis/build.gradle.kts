import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly(projects.core)
    api(project("intellij-dependency", configuration = "shadow"))
    api(project("compiler-dependency", configuration = "shadow"))
}

registerDokkaArtifactPublication("dokkaAnalysis") {
    artifactId = "dokka-analysis"
}

binaryCompatibilityValidator {
    enabled.set(false)
}
