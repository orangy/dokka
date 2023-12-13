/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled

plugins {
    id("dokkabuild.kotlin-jvm")
    `test-suite-base`
    `java-test-fixtures`
}

dependencies {
    api(projects.utilities)

    api(libs.jsoup)

    api(libs.kotlin.test)
    api(libs.junit.jupiterApi)
    api(libs.junit.jupiterParams)

    api(gradleTestKit())
}

val dokkaModules = gradle.includedBuild("dokka")
val gradlePluginClassic = gradle.includedBuild("runner-gradle-plugin-classic")

val projectsUnderTest = listOf(
    dokkaModules,
    gradlePluginClassic,
)

// fetch the Maven repos of each IncludedBuild generated by the dev.adamko.dev-publish plugin
val testMavenDirs = files(projectsUnderTest.map { it.projectDir.resolve("build/maven-dev") })

tasks.integrationTestPreparation {
    dependsOn(
        projectsUnderTest.map { it.task(":integrationTestPreparation") }
    )
}

tasks.withType<Test>().configureEach {
    dependsOn(tasks.integrationTestPreparation)
}

val templateProjectsDir = layout.projectDirectory.dir("projects")
val exampleProjectsDir = layout.projectDirectory.dir("../../examples/gradle")

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()
            dependencies {
                implementation(project())
            }

            targets.configureEach {
                testTask.configure {
                    testMavenDirs.forEach { testMavenDir -> inputs.dir(testMavenDir) }
                    systemProperty(
                        "projectLocalMavenDirs",
                        testMavenDirs.joinToString(":") { it.invariantSeparatorsPath }
                    )

//                    inputs.dir(templateProjectsDir).withPropertyName("templateProjectsDir")
                    systemProperty("templateProjectsDir", templateProjectsDir.asFile.invariantSeparatorsPath)
                    systemProperty("exampleProjectsDir", exampleProjectsDir.asFile.invariantSeparatorsPath)
                    doFirst {
                        logger.lifecycle("running $path with javaLauncher:${javaLauncher.orNull?.metadata?.javaRuntimeVersion}")
                    }
                }
            }
        }

        register<JvmTestSuite>("basicProjectTest") {
            targets.configureEach {
                testTask.configure {
                    inputs.dir(templateProjectsDir.dir("it-basic"))
                }
            }
        }
    }
    tasks.check {
        dependsOn(suites)
    }
}

kotlin {
    explicitApi = Disabled

    compilerOptions {
        allWarningsAsErrors = false
    }
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "2G"

    val useK2 = dokkaBuild.tryK2.get()

    useJUnitPlatform {
        if (useK2) excludeTags("onlyDescriptors", "onlyDescriptorsMPP")
    }

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

    systemProperty("org.jetbrains.dokka.experimental.tryK2", useK2)

    // allow inspecting projects in failing tests
    systemProperty("junit.jupiter.tempdir.cleanup.mode.default", "ON_SUCCESS")

    testLogging {
        exceptionFormat = FULL
        events(SKIPPED, FAILED)
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
