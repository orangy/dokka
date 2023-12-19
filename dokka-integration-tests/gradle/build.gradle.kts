/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled
plugins {
    id("dokkabuild.kotlin-jvm")
    `test-suite-base`
    `java-test-fixtures`
    id("dokkabuild.testing.android-setup")
    id("dokkabuild.testing.android-setup")
}

dependencies {
    api(projects.utilities)

    api(libs.jsoup)

    api(libs.kotlin.test)
    api(libs.junit.jupiterApi)
    api(libs.junit.jupiterParams)

    api(gradleTestKit())
}

kotlin {
    explicitApi = Disabled

    compilerOptions {
        allWarningsAsErrors = false
        optIn.add("kotlin.io.path.ExperimentalPathApi")
    }
}

/** A list of Included Build projects that are required for running integration tests. */
val projectsUnderTest = listOf(
    gradle.includedBuild("dokka"),
    gradle.includedBuild("runner-gradle-plugin-classic"),
)

/** the Maven repos of each IncludedBuild generated by the dev.adamko.dev-publish plugin */
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
val templateSettingsGradleKts = layout.projectDirectory.file("projects/template.settings.gradle.kts")
val androidSdkDir = templateProjectsDir.dir("ANDROID_SDK")

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

                    environment("ANDROID_HOME", androidSdkDir.asFile.invariantSeparatorsPath)

                    inputs.file(templateSettingsGradleKts)
                    systemProperty(
                        "templateSettingsGradleKts",
                        templateSettingsGradleKts.asFile.invariantSeparatorsPath,
                    )

                    doFirst {
                        logger.lifecycle("running $path with javaLauncher:${javaLauncher.orNull?.metadata?.javaRuntimeVersion}")
                    }
                }
            }

            sources { java { setSrcDirs(emptyList<String>()) } }
        }

        /**
         * Create a new [JvmTestSuite] for a Gradle project.
         *
         * @param[projectPath] path to the Gradle project that will be tested by this suite, relative to [templateProjectsDir].
         * The directory will be passed as a system property, `templateProjectDir`.
         */
        fun registerTestProjectSuite(
            name: String,
            projectPath: String,
            jvm: JavaLanguageVersion? = null,
            configure: JvmTestSuite.() -> Unit = {},
        ) {
            val templateProjectDir = templateProjectsDir.dir(projectPath)

            register<JvmTestSuite>(name) {
                targets.configureEach {
                    testTask.configure {
                        // Register the project dir as a specific input, so changes in other projects don't affect the caching of this test
                        inputs.dir(templateProjectDir)
                        // Pass the template dir in as a property, it is accessible in tests.
                        systemProperty("templateProjectDir", templateProjectDir.asFile.invariantSeparatorsPath)

                        if (jvm != null) {
                            javaLauncher = javaToolchains.launcherFor { languageVersion = jvm }
                        }
                    }
                }
                configure()
            }
        }

        // register a separate test suite for each template project, to help with Gradle caching
        registerTestProjectSuite(
            "testTemplateProjectAndroid",
            "it-android-0",
            // AGP requires JVM 11+
            jvm = JavaLanguageVersion.of(11)
        )
        registerTestProjectSuite("testTemplateProjectBasic", "it-basic")
        registerTestProjectSuite("testTemplateProjectBasicGroovy", "it-basic-groovy")
        registerTestProjectSuite("testTemplateProjectCollector", "it-collector-0")
        registerTestProjectSuite("testTemplateProjectConfiguration", "it-configuration")
        registerTestProjectSuite("testTemplateProjectJsIr", "it-js-ir-0")
        registerTestProjectSuite("testTemplateProjectMultimodule0", "it-multimodule-0")
        registerTestProjectSuite("testTemplateProjectMultimodule1", "it-multimodule-1")
        registerTestProjectSuite("testTemplateProjectMultimoduleVersioning", "it-multimodule-versioning-0")
        registerTestProjectSuite("testTemplateProjectMultiplatform", "it-multiplatform-0")
        registerTestProjectSuite("testTemplateProjectTasksExecutionStress", "it-sequential-tasks-execution-stress")
        registerTestProjectSuite("testTemplateProjectWasmBasic", "it-wasm-basic")
        registerTestProjectSuite("testTemplateProjectWasmJsWasiBasic", "it-wasm-js-wasi-basic")

        registerTestProjectSuite(
            "testExternalProjectKotlinxCoroutines",
            "coroutines/kotlinx-coroutines",
            jvm = JavaLanguageVersion.of(11) // https://github.com/Kotlin/kotlinx.coroutines/issues/3665
        ) {
            targets.configureEach {
                testTask.configure {
                    // register the whole directory as an input because it contains the git diff
                    inputs.dir(templateProjectsDir.file("coroutines"))
                }
            }
        }
        registerTestProjectSuite("testExternalProjectKotlinxSerialization", "serialization/kotlinx-serialization") {
            targets.configureEach {
                testTask.configure {
                    // register the whole directory as an input because it contains the git diff
                    inputs.dir(templateProjectsDir.file("serialization"))
                }
            }
        }
    }
    tasks.check {
        dependsOn(suites)
    }
}

val testTemplateProjectsTasks = tasks.withType<Test>().matching { it.name.startsWith("testTemplateProject") }
val testExternalProjectsTasks = tasks.withType<Test>().matching { it.name.startsWith("testExternalProject") }

//region task ordering - template projects should be tested before external projects
testTemplateProjectsTasks.configureEach {
    shouldRunAfter(tasks.test)
}
testExternalProjectsTasks.configureEach {
    shouldRunAfter(tasks.test)
    shouldRunAfter(testTemplateProjectsTasks)
}
//endregion

//region define lifecycle tasks
val testAllTemplateProjects by tasks.registering {
    description = "Lifecycle task for running all template-project tests"
    group = VERIFICATION_GROUP
    dependsOn(testTemplateProjectsTasks)
    doNotTrackState("lifecycle task, should always run")
}

val testAllExternalProjects by tasks.registering {
    description = "Lifecycle task for running all external-project tests"
    group = VERIFICATION_GROUP
    shouldRunAfter(testAllTemplateProjects)
    dependsOn(testExternalProjectsTasks)
    doNotTrackState("lifecycle task, should always run")
}
//endregion

tasks.withType<Test>().configureEach {
    maxHeapSize = "2G"

    val useK2 = dokkaBuild.tryK2.get()

    useJUnitPlatform {
        if (useK2) excludeTags("onlyDescriptors", "onlyDescriptorsMPP")
    }

    systemProperty("org.jetbrains.dokka.experimental.tryK2", useK2)

    setForkEvery(1)
    dokkaBuild.integrationTestParallelism.orNull?.let {
        maxParallelForks = it
    }

    environment("isExhaustive", dokkaBuild.integrationTestExhaustive)

    // allow inspecting projects in temporary dirs after a test fails
    systemProperty(
        "junit.jupiter.tempdir.cleanup.mode.default",
        dokkaBuild.isCI.map { isCi -> if (isCi) "ALWAYS" else "ON_SUCCESS" }.get(),
    )

    testLogging {
        exceptionFormat = FULL
        events(SKIPPED, FAILED)
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

val templateProjectsDir = layout.projectDirectory.dir("projects")
val androidSdkDir = providers
    // first try getting pre-installed SDK (e.g. via GitHub step setup-android)
    .environmentVariable("ANDROID_SDK_ROOT").map(::File)
    .orElse(providers.environmentVariable("ANDROID_HOME").map(::File))
    // else get the project-local SDK
    .orElse(templateProjectsDir.dir("ANDROID_SDK").asFile)

tasks.withType<Test>().configureEach {
    environment("ANDROID_HOME", androidSdkDir.get().invariantSeparatorsPath)
}

tasks.createAndroidLocalPropertiesFiles {
    // The names of android projects that require a local.properties file
    val androidProjects = setOf(
        "it-android-0",
    )
    // find all Android projects that need a local.properties file
    androidProjectsDirectories.from(
        templateProjectsDir.asFileTree.matching {
            include { it.isDirectory && it.name in androidProjects }
        }
    )
    androidSdkDirPath.set(androidSdkDir.map { it.invariantSeparatorsPath })
}

tasks.integrationTestPreparation {
    dependsOn(tasks.createAndroidLocalPropertiesFiles)
}
