import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

repositories {
    // Remove it when wasm target will be published into public maven repository
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
}

kotlin {
    wasm()
    sourceSets {
        val wasmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4-wasm1")
                implementation("org.jetbrains.kotlinx:atomicfu-wasm:0.18.5-wasm1")
            }
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        configureEach {
            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/"))
            }
        }
    }
}

// HACK: some dependencies (coroutines -wasm0 and atomicfu -wasm0) reference deleted *-dev libs
configurations.all {
    val conf = this
    resolutionStrategy.eachDependency {
        if (requested.version == "1.8.20-dev-3308") {
            println("Substitute deleted version ${requested.module}:${requested.version} for ${conf.name}")
            useVersion(project.properties["dokka_it_kotlin_version"] as String)
        }
    }
}