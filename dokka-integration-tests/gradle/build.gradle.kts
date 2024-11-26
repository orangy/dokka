/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UnstableApiUsage")

import dokkabuild.tasks.GitCheckoutTask
import dokkabuild.utils.DokkaGradleExampleDirectoriesSource
import dokkabuild.utils.systemProperty
import dokkabuild.utils.uppercaseFirstChar
import org.gradle.api.tasks.PathSensitivity.*

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.test-integration")
    id("dokkabuild.dev-maven-publish")
    alias(libs.plugins.kotlinxSerialization)
}

dependencies {
    val dokkaVersion = project.version.toString()

    api(projects.utilities)

    api(libs.jsoup)

    api(libs.kotlin.test)
    api(libs.junit.jupiterApi)
    api(libs.junit.jupiterParams)
    api(libs.kotest.assertionsCore)
    api(gradleTestKit())

    api(testFixtures("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion"))
    api(platform(libs.kotest.bom))
    api(libs.kotest.assertionsCore)
    api(libs.kotest.assertionsJson)

    implementation(platform(libs.kotlinxSerialization.bom))
    implementation(libs.kotlinxSerialization.json)

    // We're using Gradle included-builds and dependency substitution, so we
    // need to use the Gradle project name, *not* the published Maven artifact-id
    devPublication("org.jetbrains.dokka:plugin-all-modules-page:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-kotlin-api:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-kotlin-descriptors:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-kotlin-symbols:$dokkaVersion")
    devPublication("org.jetbrains.dokka:analysis-markdown-jb:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-android-documentation:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-base:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-base-test-utils:$dokkaVersion")
    devPublication("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-gfm:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-gfm-template-processing:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-javadoc:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-jekyll:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-jekyll-template-processing:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-kotlin-as-java:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-mathjax:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-templating:$dokkaVersion")
    devPublication("org.jetbrains.dokka:plugin-versioning:$dokkaVersion")

    devPublication("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.io.path.ExperimentalPathApi")
    }
}

tasks.withType<Test>().configureEach {
    val enableDebug = providers.environmentVariable("ENABLE_DEBUG")
    inputs.property("enableDebug", enableDebug).optional(true)
    environment("ENABLE_DEBUG", enableDebug.getOrElse("false"))

    systemProperty("hostGradleUserHome", gradle.gradleUserHomeDir.invariantSeparatorsPath)
}


val templateSettingsGradleKts = layout.projectDirectory.file("projects/template.settings.gradle.kts")
val templateProjectsDir = layout.projectDirectory.dir("projects")

// register a separate test suite for each 'template' project
registerTestProjectSuite(
    "testTemplateProjectAndroid",
    "it-android-0",
    jvm = JavaLanguageVersion.of(17), // AGP requires JVM 17+
) {
    targets.configureEach {
        testTask.configure {
            // Don't register ANDROID_HOME as a Task input, because the path is different on everyone's machine,
            // which means Gradle will never be able to cache the task.
            dokkaBuild.androidSdkDir.orNull?.let { androidSdkDir ->
                environment("ANDROID_HOME", androidSdkDir)
            }
        }
    }
}
registerTestProjectSuite("testTemplateProjectBasic", "it-basic")
registerTestProjectSuite("testTemplateProjectBasicGroovy", "it-basic-groovy")
registerTestProjectSuite("testTemplateProjectCollector", "it-collector-0")
registerTestProjectSuite("testTemplateProjectConfiguration", "it-configuration")
registerTestProjectSuite("testTemplateProjectJsIr", "it-js-ir-0")
registerTestProjectSuite("testTemplateProjectMultimodule0", "it-multimodule-0")
registerTestProjectSuite("testTemplateProjectMultimodule1", "it-multimodule-1")
registerTestProjectSuite("testTemplateProjectMultimoduleVersioning", "it-multimodule-versioning-0")
registerTestProjectSuite("testTemplateProjectMultimoduleInterModuleLinks", "it-multimodule-inter-module-links")
registerTestProjectSuite("testTemplateProjectMultiplatform", "it-multiplatform-0")
registerTestProjectSuite("testTemplateProjectMultiplatformMultimodule", "it-multiplatform-multimodule")
registerTestProjectSuite("testTemplateProjectTasksExecutionStress", "it-sequential-tasks-execution-stress")
registerTestProjectSuite("testTemplateProjectWasmBasic", "it-wasm-basic")
registerTestProjectSuite("testTemplateProjectWasmJsWasiBasic", "it-wasm-js-wasi-basic")

registerTestProjectSuite(
    "testExternalProjectKotlinxCoroutines",
    "coroutines/kotlinx-coroutines",
    jvm = JavaLanguageVersion.of(11) // kotlinx.coroutines requires JVM 11+ https://github.com/Kotlin/kotlinx.coroutines/issues/3665
) {
    targets.configureEach {
        testTask.configure {
            dependsOn(checkoutKotlinxCoroutines)
            // register the whole directory as an input because it contains the git diff
            inputs
                .dir(templateProjectsDir.file("coroutines"))
                .withPropertyName("coroutinesProjectDir")
        }
    }
}
registerTestProjectSuite(
    "testExternalProjectKotlinxSerialization",
    "serialization/kotlinx-serialization",
    jvm = JavaLanguageVersion.of(11) // https://github.com/Kotlin/kotlinx.serialization/blob/1116f5f13a957feecda47d5e08b0aa335fc010fa/gradle/configure-source-sets.gradle#L9
) {
    targets.configureEach {
        testTask.configure {
            dependsOn(checkoutKotlinxSerialization)
            // register the whole directory as an input because it contains the git diff
            inputs
                .dir(templateProjectsDir.file("serialization"))
                .withPropertyName("serializationProjectDir")
        }
    }
}
registerTestProjectSuite("testUiShowcaseProject", "ui-showcase")

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

    testing.suites.register<JvmTestSuite>(name) {
        targets.configureEach {
            testTask.configure {
                // Pass the template dir in as a property, so it is accessible in tests.
                systemProperty
                    .inputDirectory("templateProjectDir", templateProjectDir)
                    .withPathSensitivity(RELATIVE)

                systemProperty
                    .inputFile("templateSettingsGradleKts", templateSettingsGradleKts)
                    .withPathSensitivity(NAME_ONLY)

                if (jvm != null) {
                    javaLauncher = javaToolchains.launcherFor { languageVersion = jvm }
                }
            }
        }
        configure()
    }
}

testing.suites.named<JvmTestSuite>("test") {
    targets.configureEach {
        testTask {
            systemProperty
                .inputDirectory("templateProjectsDir", templateProjectsDir)
                .withPathSensitivity(RELATIVE)

            // Don't register ANDROID_HOME as a Task input, because the path is different on everyone's machine,
            // which means Gradle will never be able to cache the task.
            dokkaBuild.androidSdkDir.orNull?.let { androidSdkDir ->
                environment("ANDROID_HOME", androidSdkDir)
            }

            // Use a stable Java version for running Gradle in integration tests.
            // There's no need to parameterise the version, to re-run the tests with different JDKs.
            // There are a few reasons for this:
            // - Some tests use AGP 8, which requires Gradle 17+.
            // - DGP functional tests are already run with Java 8, so we don't need to re-test Java 8 compatibility here.
            // - The JDK used to run Gradle doesn't affect the Dokka output. The Java Toolchain used to compile
            //   the code in test projects does affect the output... but that can be parameterised in the
            //   individual tests if necessary.
            javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }
        }
    }
}

val checkoutKotlinxCoroutines by tasks.registering(GitCheckoutTask::class) {
    uri = "https://github.com/Kotlin/kotlinx.coroutines.git"
    commitId = "b78bbf518bd8e90e9ed2133ebdacc36441210cd6"
    destination = templateProjectsDir.dir("coroutines/kotlinx-coroutines")
}

val checkoutKotlinxSerialization by tasks.registering(GitCheckoutTask::class) {
    uri = "https://github.com/Kotlin/kotlinx.serialization.git"
    commitId = "ed1b05707ec27f8864c8b42235b299bdb5e0015c"
    destination = templateProjectsDir.dir("serialization/kotlinx-serialization")
}

testing {
    suites.withType<JvmTestSuite>().configureEach {
        targets.configureEach {
            testTask.configure {
                devMavenPublish.configureTask(this)

                // temp workaround, remove when all `testTemplateProject*` source sets are removed
                mustRunAfter(tasks.withType<GitCheckoutTask>())
            }
        }
    }
    val testExampleProjects by suites.registering(JvmTestSuite::class) {
        val exampleGradleProjectsDir = projectDir.resolve("../../examples/gradle-v2")

        targets.configureEach {
            testTask {
                systemProperty
                    .inputDirectory("exampleGradleProjectsDir", exampleGradleProjectsDir)
                    .withPathSensitivity(RELATIVE)

                val expectedDataDir = layout.projectDirectory.dir("src/testExampleProjects/expectedData")
                systemProperty
                    .inputDirectory("expectedDataDir", expectedDataDir)
                    .withPathSensitivity(RELATIVE)

                // Disable parallel on CI. TeamCity OOMs when the tests are run in parallel.
                systemProperty.inputProperty("junit.jupiter.execution.parallel.enabled", dokkaBuild.isCI.map { !it })
                systemProperty("junit.jupiter.execution.parallel.mode.default", "CONCURRENT")
            }
        }

        val exampleProjectDirs = providers.of(DokkaGradleExampleDirectoriesSource::class) {
            parameters.exampleGradleProjectsDir = exampleGradleProjectsDir
        }.get()

        exampleProjectDirs.forEach { exampleProjectDir ->
            val exampleProjectName = exampleProjectDir.name
            val prettyName = exampleProjectName
                .split("-")
                .joinToString("") { it.uppercaseFirstChar() }

            targets.register("test$prettyName") {
                testTask {
                    description = "Only test $exampleProjectName"
                    group = "verification - example projects"
                    systemProperty.inputProperty("exampleProjectFilter", exampleProjectName)

                    systemProperty
                        .inputDirectory("exampleGradleProjectDir $exampleProjectName", exampleProjectDir)
                        .withPathSensitivity(NONE)
                }
            }
        }
    }
}
