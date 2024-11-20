plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    id("org.jetbrains.dokka")

    if ("/* %{KGP_VERSION} */".startsWith("2.")) {
        id("org.jetbrains.kotlin.plugin.compose")
    }
    alias(libs.plugins.compose.multiplatform)
}

group = "org.dokka.it.android.kmp"
version = "1.0.0"

android {
    namespace = "org.dokka.it.android.kmp"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":core"))
                implementation(compose.material3)
            }
        }
    }
}
