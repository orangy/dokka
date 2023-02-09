import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

kotlin {
    jvm()
    linuxX64("linux")
    macosX64("macos")
    js()
    //TODO Add wasm when kx.coroutines will be supported and published into the main repo
    sourceSets {
        val commonMain by sourceSets.getting
        val linuxMain by sourceSets.getting
        val macosMain by sourceSets.getting
        val desktopMain by sourceSets.creating {
            dependsOn(commonMain)
            linuxMain.dependsOn(this)
            macosMain.dependsOn(this)
        }
        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
            }
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        configureEach {
            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/"))
                //packageListUrl.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/package-list"))
            }
        }
    }
}
