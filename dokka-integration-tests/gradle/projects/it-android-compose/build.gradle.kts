plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false

    if ("/* %{KGP_VERSION} */".startsWith("2.")) {
        id("org.jetbrains.kotlin.plugin.compose") version "/* %{KGP_VERSION} */" apply false
    }
    alias(libs.plugins.compose.multiplatform) apply false

    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
}

dependencies {
    dokka(project(":core"))
    dokka(project(":material3"))
}
