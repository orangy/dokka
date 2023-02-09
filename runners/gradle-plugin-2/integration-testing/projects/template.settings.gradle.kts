@file:Suppress("LocalVariableName", "UnstableApiUsage")

pluginManagement {
    val dokka_it_kotlin_version: String by settings
    val dokka_it_android_gradle_plugin_version: String? by settings

    plugins {
        id("org.jetbrains.kotlin.js") version dokka_it_kotlin_version
        id("org.jetbrains.kotlin.jvm") version dokka_it_kotlin_version
        id("org.jetbrains.kotlin.android") version dokka_it_kotlin_version
        id("org.jetbrains.kotlin.multiplatform") version dokka_it_kotlin_version
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.dokka") {
                useModule("org.jetbrains.dokka:dokka-gradle-plugin:for-integration-tests-SNAPSHOT")
            }

            if (requested.id.id == "com.android.library") {
                useModule("com.android.tools.build:gradle:$dokka_it_android_gradle_plugin_version")
            }

            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:$dokka_it_android_gradle_plugin_version")
            }
        }
    }
    repositories {
        mavenLocal()
        maven("https://cache-redirector.jetbrains.com/jcenter.bintray.com")
        mavenCentral()
        gradlePluginPortal()
        google()
        maven("https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-eap")
        maven("https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlin-dev")
    }
}
