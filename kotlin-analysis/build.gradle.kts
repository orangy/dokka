import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly(project(":core"))
    api(project("intellij-dependency", configuration = "shadow"))
    api(project("compiler-dependency", configuration = "shadow"))
}

registerDokkaArtifactPublication("dokkaAnalysis") {
    artifactId = "dokka-analysis"
}
