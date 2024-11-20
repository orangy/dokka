import com.android.build.api.dsl.SettingsExtension

pluginManagement {
    repositories {
        /* %{DOKKA_IT_MAVEN_REPO}% */
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        /* %{DOKKA_IT_MAVEN_REPO}% */
        mavenCentral()
        google()
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/public/p/compose/dev") {
            name = "JetBrainsComposeDev"
            // required for org.jetbrains.compose.compiler:compiler:1.5.15
        }
    }
}

plugins {
    id("com.android.settings") version "/* %{AGP_VERSION} */"
}

rootProject.name = "ComposeMenuProvider"

include(
    ":core",
    ":material3",
)

configure<SettingsExtension> {
    buildToolsVersion = "34.0.0"
    compileSdk = 34
    minSdk = 28
}
