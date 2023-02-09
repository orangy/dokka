import org.jetbrains.SetupMaven
import org.jetbrains.dependsOnMavenLocalPublication

plugins {
    org.jetbrains.conventions.`dokka-integration-test`
}

evaluationDependsOn(":runners:maven-plugin")

dependencies {
    implementation(project(":integration-tests"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
}

tasks.integrationTest {
    dependsOnMavenLocalPublication()

    val setupMavenTask = project(":runners:maven-plugin").tasks.withType<SetupMaven>().single()
    dependsOn(setupMavenTask)

    val dokka_version: String by project
    environment("DOKKA_VERSION", dokka_version)
    environment("MVN_BINARY_PATH", setupMavenTask.mvn.absolutePath)
}
