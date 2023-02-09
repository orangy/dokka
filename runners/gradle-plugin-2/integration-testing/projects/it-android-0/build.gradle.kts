plugins {
    id("com.android.library")
    id("org.jetbrains.dokka")
    kotlin("android")
}

apply(from = "../template.root.gradle.kts")

android {
    defaultConfig {
        minSdkVersion(21)
        setCompileSdkVersion(29)
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.appcompat:appcompat:1.1.0")
}

