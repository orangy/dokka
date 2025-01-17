/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
    val dokka_it_kotlin_version: String by settings
    val dokka_it_dokka_version: String by settings
    val dokka_it_android_gradle_plugin_version: String? by settings

    logger.quiet("Gradle version: ${gradle.gradleVersion}")
    logger.quiet("Kotlin version: $dokka_it_kotlin_version")
    logger.quiet("Dokka version: $dokka_it_dokka_version")
    logger.quiet("Android version: $dokka_it_android_gradle_plugin_version")

    plugins {
        id("org.jetbrains.kotlin.js") version dokka_it_kotlin_version
        id("org.jetbrains.kotlin.jvm") version dokka_it_kotlin_version
        id("org.jetbrains.kotlin.android") version dokka_it_kotlin_version
        id("org.jetbrains.kotlin.multiplatform") version dokka_it_kotlin_version
        id("org.jetbrains.dokka") version dokka_it_dokka_version
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.dokka") {
                useModule("org.jetbrains.dokka:dokka-gradle-plugin:$dokka_it_dokka_version")
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
        /* %{DOKKA_IT_MAVEN_REPO}% */
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        /* %{DOKKA_IT_MAVEN_REPO}% */
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") {
            content {
                includeGroup("org.jetbrains.kotlinx")
            }
        }
        // Remove when Kotlin/Wasm is published into public Maven repository
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")

        // Declare the Node.js & Yarn download repositories - workaround for https://youtrack.jetbrains.com/issue/KT-51379
        exclusiveContent {
            forRepository {
                ivy("https://nodejs.org/dist/") {
                    name = "Node Distributions at $url"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("org.nodejs", "node") }
                }
            }
            filter { includeGroup("org.nodejs") }
        }

        exclusiveContent {
            forRepository {
                ivy("https://github.com/yarnpkg/yarn/releases/download") {
                    name = "Yarn Distributions at $url"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("com.yarnpkg", "yarn") }
                }
            }
            filter { includeGroup("com.yarnpkg") }
        }

        // Declare Kotlin/Native dependencies - workaround for https://youtrack.jetbrains.com/issue/KT-51379
        // Remove this repo when the only supported KGP version is above 2.0.0
        ivy("https://download.jetbrains.com/kotlin/native/builds") {
            name = "Kotlin Native"
            patternLayout {

                // example download URLs:
                // https://download.jetbrains.com/kotlin/native/builds/releases/1.7.20/linux-x86_64/kotlin-native-prebuilt-linux-x86_64-1.7.20.tar.gz
                // https://download.jetbrains.com/kotlin/native/builds/releases/1.7.20/windows-x86_64/kotlin-native-prebuilt-windows-x86_64-1.7.20.zip
                // https://download.jetbrains.com/kotlin/native/builds/releases/1.7.20/macos-x86_64/kotlin-native-prebuilt-macos-x86_64-1.7.20.tar.gz
                listOf(
                    "macos-x86_64",
                    "macos-aarch64",
                    "osx-x86_64",
                    "osx-aarch64",
                    "linux-x86_64",
                    "windows-x86_64"
                ).forEach { os ->
                    listOf("dev", "releases").forEach { stage ->
                        artifact("$stage/[revision]/$os/[artifact]-[revision].[ext]")
                    }
                }
            }
            metadataSources { artifact() }
            content { includeModuleByRegex(".*", ".*kotlin-native-prebuilt.*") }
        }
    }
}
