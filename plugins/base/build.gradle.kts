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

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    compileOnly(project(":kotlin-analysis"))
    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")

    val jackson_version: String by project
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    val jackson_databind_version: String by project
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_databind_version") {
            because("CVE-2022-42003")
        }
    }

    val freemarker_version: String by project
    implementation("org.freemarker:freemarker:$freemarker_version")

    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))

    val kotlinx_html_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")

    testImplementation(project(":kotlin-analysis"))
    testImplementation(project(":test-utils"))
    testImplementation(project(":core:test-api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}

val projectDistDir = project(":plugins:base:frontend").file("dist")
val generateFrontendFiles = tasks.getByPath(":plugins:base:frontend:generateFrontendFiles")

val copyJsFiles by tasks.registering(Copy::class){
    from(projectDistDir){
        include("*.js")
    }
    dependsOn(generateFrontendFiles)
    destinationDir = File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/scripts")
}

val copyCssFiles by tasks.registering(Copy::class){
    from(projectDistDir){
        include("*.css")
    }
    dependsOn(generateFrontendFiles)
    destinationDir = File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/styles")
}

val copyFrontend by tasks.registering {
    dependsOn(copyJsFiles, copyCssFiles)
}

tasks {
    processResources {
        dependsOn(copyFrontend)
    }

    sourcesJar {
        dependsOn(processResources)
    }

    test {
        maxHeapSize = "4G"
    }
}

registerDokkaArtifactPublication("dokkaBase") {
    artifactId = "dokka-base"
}
