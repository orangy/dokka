plugins {
    id("com.android.library") version "/* %{AGP_VERSION} */"
    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
    kotlin("android") version "/* %{KGP_VERSION} */"
}

android {
    namespace = "org.jetbrains.dokka.it.android"
    defaultConfig {
        minSdkVersion(21)
        setCompileSdkVersion(29)
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.1.0")
}
