import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.withType<DokkaTask>().configureEach {
    moduleName.set("!Module B!")
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
    }
}
