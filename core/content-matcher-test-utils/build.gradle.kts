plugins {
    org.jetbrains.conventions.`kotlin-jvm`
}

dependencies {
    implementation(project(":core:test-api"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
}
