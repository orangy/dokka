/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("UnstableApiUsage")

import dokkabuild.tasks.GitCheckoutTask
import dokkabuild.utils.systemProperty
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.api.tasks.PathSensitivity.RELATIVE

plugins {
    id("dokkabuild.kotlin-jvm")
    id("dokkabuild.dev-maven-publish")
    id("dokkabuild.setup-maven-cli")
    id("dokkabuild.test-integration")
    `jvm-test-suite`
}

dependencies {
    api(projects.utilities)

    api(kotlin("test-junit5"))
    api(libs.junit.jupiterApi)

    val dokkaVersion = project.version.toString()
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

    devPublication("org.jetbrains.dokka:runner-maven-plugin:$dokkaVersion")
}

val templateProjectsDir = layout.projectDirectory.dir("projects")

tasks.withType<Test>().configureEach {
    dependsOn(tasks.installMavenBinary)
    systemProperty.inputFile("mavenBinaryFile", mavenCliSetup.mvn)
        .withPathSensitivity(NAME_ONLY)
}

registerTestProjectSuite("testTemplateProjectMaven", "it-maven")
registerTestProjectSuite("testExternalProjectBioJava", "biojava/biojava") {
    targets.configureEach {
        testTask.configure {
            dependsOn(checkoutBioJava)
            // register the whole directory as an input because it contains the git diff
            inputs
                .dir(templateProjectsDir.file("biojava"))
                .withPropertyName("biojavaProjectDir")
                .withPathSensitivity(RELATIVE)
        }
    }
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

    testing.suites.register<JvmTestSuite>(name) {
        targets.configureEach {
            testTask.configure {
                // Pass the template dir in as a property, it is accessible in tests.
                systemProperty
                    .inputDirectory("templateProjectDir", templateProjectDir)
                    .withPathSensitivity(RELATIVE)

                devMavenPublish.configureTask(this)

                if (jvm != null) {
                    javaLauncher = javaToolchains.launcherFor { languageVersion = jvm }
                }
            }
        }
        configure()
    }
}

val checkoutBioJava by tasks.registering(GitCheckoutTask::class) {
    uri = "https://github.com/biojava/biojava.git"
    commitId = "059fbf1403d0704801df1427b0ec925102a645cd"
    destination = templateProjectsDir.dir("biojava/biojava")
}
