package org.jetbrains.conventions

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

val integrationTestSourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.runtimeOnly.get())
}

val integrationTest by tasks.registering(Test::class) {
    maxHeapSize = "2G"
    description = "Runs integration tests."
    group = "verification"
    useJUnit()

    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath

    setForkEvery(1)
    project.properties["dokka_integration_test_parallelism"]?.toString()?.toIntOrNull()?.let { parallelism ->
        maxParallelForks = parallelism
    }
    environment(
        "isExhaustive",
        project.properties["dokka_integration_test_is_exhaustive"]?.toString()?.toBoolean()
            ?: System.getenv("DOKKA_INTEGRATION_TEST_IS_EXHAUSTIVE")?.toBoolean()
            ?: false.toString()
    )

    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.check {
    dependsOn(integrationTest)
}
