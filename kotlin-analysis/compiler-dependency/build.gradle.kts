import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(libs.kotlin.compiler)
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-kotlin-analysis-compiler-$dokka_version.jar")
        archiveClassifier.set("")
        exclude("com/intellij/")
    }
}

registerDokkaArtifactPublication("kotlinAnalysisCompiler") {
    artifactId = "kotlin-analysis-compiler"
    component = Shadow
}

binaryCompatibilityValidator {
    enabled.set(false)
}
