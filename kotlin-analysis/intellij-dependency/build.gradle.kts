import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("com.github.johnrengelman.shadow")
}

repositories {
    // Override the shared repositories defined in the root settings.gradle.kts
    // These repositories are very specific and are not needed in other projects
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/snapshots") {
        mavenContent { snapshotsOnly() }
    }
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://www.myget.org/F/rd-snapshots/maven/")
}

val intellijCore: Configuration by configurations.creating

fun intellijCoreAnalysis() = zipTree(intellijCore.singleFile).matching {
    include("intellij-core.jar")
}

val jpsStandalone: Configuration by configurations.creating

fun jpsModel() = zipTree(jpsStandalone.singleFile).matching {
    include("jps-model.jar")
    include("aalto-xml-*.jar")
}

dependencies {
    api(libs.kotlin.idePlugin.common)
    api(libs.kotlin.idePlugin.idea) {
        isTransitive = false
    }
    api(libs.kotlin.idePlugin.core)
    api(libs.kotlin.idePlugin.native)

    @Suppress("UnstableApiUsage")
    intellijCore(libs.jetbrains.intellij.core)
    implementation(intellijCoreAnalysis())

    @Suppress("UnstableApiUsage")
    jpsStandalone(libs.jetbrains.intellij.jpsStandalone)
    implementation(jpsModel())
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-kotlin-analysis-intellij-$dokka_version.jar")
        archiveClassifier.set("")

        exclude("colorScheme/**")
        exclude("fileTemplates/**")
        exclude("inspectionDescriptions/**")
        exclude("intentionDescriptions/**")
        exclude("tips/**")
        exclude("messages/**")
        exclude("src/**")
        exclude("**/*.kotlin_metadata")
        exclude("**/*.kotlin_builtins")
    }
}

registerDokkaArtifactPublication("kotlinAnalysisIntelliJ") {
    artifactId = "kotlin-analysis-intellij"
    component = Shadow
}

binaryCompatibilityValidator {
    enabled.set(false)
}
