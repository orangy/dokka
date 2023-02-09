plugins {
    id("org.jetbrains.dokka")
    kotlin("js")
}

apply(from = "../template.root.gradle.kts")

kotlin {
    js(IR) {
        browser()
        nodejs()
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(npm("is-sorted", "1.0.5"))

    val reactVersion = properties["react_version"]
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$reactVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$reactVersion")
}