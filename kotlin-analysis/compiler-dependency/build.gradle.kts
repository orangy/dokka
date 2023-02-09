import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    org.jetbrains.conventions.`kotlin-jvm`
    org.jetbrains.conventions.`maven-publish`
    id("com.github.johnrengelman.shadow")
}

dependencies {
    val kotlin_version: String by project
    api("org.jetbrains.kotlin:kotlin-compiler:$kotlin_version")
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
