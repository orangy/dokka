plugins {
    org.jetbrains.conventions.`kotlin-jvm`
}

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":test-utils"))
    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.12.0.202106070339-r")
}
