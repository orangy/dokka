plugins {
    `embedded-kotlin`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}


dependencies {
    val dokkaVersion = "2.0.20-SNAPSHOT"
    compileOnly("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
