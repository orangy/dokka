package org.jetbrains.dokka.gradle

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.paths.shouldBeAFile
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs
import org.jetbrains.dokka.gradle.utils.*
import org.junit.jupiter.api.Test

class MultiModuleFunctionalTest {

    private val project = gradleKtsProjectTest("multi-module-hello-goodbye") {

        settingsGradleKts += """
                |
                |include(":subproject-hello")
                |include(":subproject-goodbye")
            """.trimMargin()

        buildGradleKts = """
                |import org.jetbrains.dokka.gradle.tasks.DokkaConfigurationTask
                |import org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs
                |import org.jetbrains.dokka.*
                |
                |plugins {
                |    // Kotlin plugin shouldn't be necessary here, but without it Dokka errors
                |    // with ClassNotFound KotlinPluginExtension... very weird
                |    kotlin("jvm") version "1.7.20" apply false
                |    id("org.jetbrains.dokka2") version "2.0.0"
                |}
                |
                |dependencies {
                |    dokka(project(":subproject-hello"))
                |    dokka(project(":subproject-goodbye"))
                |}
                |
                |//tasks.withType<DokkaConfigurationTask>().configureEach {
                |//    sourceSets.add(
                |//        DokkaConfigurationKxs.DokkaSourceSetKxs(
                |//            displayName = "The Root Project",
                |//            sourceSetID = DokkaSourceSetID("moduleName", "main"),
                |//            classpath = emptyList(),
                |//            sourceRoots = setOf(file("src/main/kotlin")),
                |//            dependentSourceSets = emptySet(),
                |//            samples = emptySet(),
                |//            includes = emptySet(),
                |//            documentedVisibilities = DokkaConfiguration.Visibility.values().toSet(),
                |//            reportUndocumented = false,
                |//            skipEmptyPackages = true,
                |//            skipDeprecated = false,
                |//            jdkVersion = 8,
                |//            sourceLinks = emptySet(),
                |//            perPackageOptions = emptyList(),
                |//            externalDocumentationLinks = emptySet(),
                |//            languageVersion = null,
                |//            apiVersion = null,
                |//            noStdlibLink = false,
                |//            noJdkLink = false,
                |//            suppressedFiles = emptySet(),
                |//            analysisPlatform = org.jetbrains.dokka.Platform.DEFAULT,
                |//        )
                |//    )
                |//}
            """.trimMargin()

//        createKotlinFile(
//            "src/main/kotlin/Dummy.kt", """
//                package com.project.dummy
//
//                /** Dummy class - this is only here to trigger Dokka */
//                class Dummy {
//                    val nothing: String = ""
//                }
//            """.trimIndent()
//        )

        createKtsFile(
            "subproject-hello/build.gradle.kts",
            """
                |//import org.jetbrains.dokka.gradle.tasks.DokkaConfigurationTask
                |//import org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs
                |//import org.jetbrains.dokka.*
                |
                |plugins {
                |    kotlin("jvm") version "1.7.20"
                |    id("org.jetbrains.dokka2") version "2.0.0"
                |}
                |
                |// TODO copy the DSL from the old plugin
                |//tasks.withType<DokkaConfigurationTask>().configureEach {
                |    //dokkaSourceSets.create("Hello Subproject") {
                |       // sourceSetID = DokkaSourceSetID("moduleName", "main")
                |       // classpath = emptyList()
                |       // sourceRoots = setOf(file("src/main/kotlin"))
                |       // dependentSourceSets = emptySet()
                |       // samples = emptySet()
                |       // includes = emptySet()
                |       // documentedVisibilities = DokkaConfiguration.Visibility.values().toSet()
                |       // reportUndocumented = false
                |       // skipEmptyPackages = true
                |       // skipDeprecated = false
                |       // jdkVersion = 8
                |       // sourceLinks = emptySet()
                |       // perPackageOptions = emptyList()
                |       // externalDocumentationLinks = emptySet()
                |       // languageVersion = null
                |       // apiVersion = null
                |       // noStdlibLink = false
                |       // noJdkLink = false
                |       // suppressedFiles = emptySet()
                |       // analysisPlatform = org.jetbrains.dokka.Platform.DEFAULT
                |    //}
                |//}
                """.trimMargin()
        )

        createKotlinFile(
            "subproject-hello/src/main/kotlin/Hello.kt", """
                |package com.project.hello
                |
                |/** The Hello class */
                |class Hello {
                |    /** prints `Hello` to the console */  
                |    fun sayHello() = println("Hello")
                |}
            """.trimMargin()
        )

        createKtsFile(
            "subproject-goodbye/build.gradle.kts",
            """
                |
                |//import org.jetbrains.dokka.gradle.tasks.DokkaConfigurationTask
                |//import org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs
                |//import org.jetbrains.dokka.*
                |
                |plugins {
                |    kotlin("jvm") version "1.7.20"
                |    id("org.jetbrains.dokka2") version "2.0.0"
                |}
                |
                |logger.lifecycle("with kotlin extension " + kotlin::class.toString())
                |
                |//tasks.withType<DokkaConfigurationTask>().configureEach {
                |   // dokkaSourceSets.create("Goodbye Subproject") {}
                |//    sourceSets.add(
                |//        DokkaConfigurationKxs.DokkaSourceSetKxs(
                |//            displayName = "My Subproject",
                |//            sourceSetID = DokkaSourceSetID("moduleName", "main"),
                |//            classpath = emptyList(),
                |//            sourceRoots = setOf(file("src/main/kotlin")),
                |//            dependentSourceSets = emptySet(),
                |//            samples = emptySet(),
                |//            includes = emptySet(),
                |//            documentedVisibilities = DokkaConfiguration.Visibility.values().toSet(),
                |//            reportUndocumented = false,
                |//            skipEmptyPackages = true,
                |//            skipDeprecated = false,
                |//            jdkVersion = 8,
                |//            sourceLinks = emptySet(),
                |//            perPackageOptions = emptyList(),
                |//            externalDocumentationLinks = emptySet(),
                |//            languageVersion = null,
                |//            apiVersion = null,
                |//            noStdlibLink = false,
                |//            noJdkLink = false,
                |//            suppressedFiles = emptySet(),
                |//            analysisPlatform = org.jetbrains.dokka.Platform.DEFAULT,
                |//        )
                |//    )
                |//}
            """.trimMargin()
        )

        createKotlinFile(
            "subproject-goodbye/src/main/kotlin/Goodbye.kt", """
                |package com.project.goodbye
                |
                |/** The Goodbye class */
                |class Goodbye {
                |    /** prints a goodbye message to the console */  
                |    fun sayHello() = println("Goodbye!")
                |}
            """.trimMargin()
        )
    }

    //    @Test
    fun `expect subproject generates JavaDoc site`() {
        val build = project.runner
            .withArguments(":subproject:dokkaGenerate", "--stacktrace", "--info")
            .forwardOutput()
            .build()

        build.output shouldContain "BUILD SUCCESSFUL"
        build.output shouldContain "Generation completed successfully"


        project.projectDir.resolve("subproject/build/dokka/html/com/project/hello/Hello.html").shouldBeAFile()
        project.projectDir.resolve("subproject/build/dokka/html/index.html").shouldBeAFile()
        project.projectDir.resolve("subproject/build/dokka/html/dokka_configuration.json").shouldBeAFile()
        project.projectDir.resolve("subproject/build/dokka/html/element-list").shouldBeAFile()
        project.projectDir.resolve("subproject/build/dokka/html/element-list").toFile().readText().shouldContain(
            """
            ${'$'}dokka.format:javadoc-v1
            ${'$'}dokka.linkExtension:html

            com.project.hello
        """.trimIndent()
        )
    }

    @Test
    fun `expect root project aggregates`() {
        val build = project.runner
            .withArguments("clean", ":dokkaGenerate", "--stacktrace", "--info")
            .forwardOutput()
            .build()

        build.output shouldContain "BUILD SUCCESSFUL"
        build.output shouldContain "Generation completed successfully"

//        project.projectDir.toFile().walk().forEach { println(it) }

//        project.projectDir.resolve("subproject/build/dokka-output/com/project/hello/Hello.html").shouldBeAFile()
//        project.projectDir.resolve("subproject/build/dokka-output/index.html").shouldBeAFile()
//        project.projectDir.resolve("subproject/build/dokka-config/dokka_configuration.json").shouldBeAFile()
//        project.projectDir.resolve("subproject/build/dokka-output/element-list").shouldBeAFile()
//        project.projectDir.resolve("subproject/build/dokka-output/element-list").toFile().readText().shouldContain(
//            """
//            ${'$'}dokka.format:javadoc-v1
//            ${'$'}dokka.linkExtension:html
//
//            com.project.hello
//        """.trimIndent()
//        )

        val dokkaConfigurationFile = project.projectDir.resolve("build/dokka-config/html/dokka_configuration.json")
        dokkaConfigurationFile.shouldExist()
        dokkaConfigurationFile.shouldBeAFile()
        @OptIn(ExperimentalSerializationApi::class)
        val dokkaConfiguration = kotlinx.serialization.json.Json.decodeFromStream(
            DokkaConfigurationKxs.serializer(),
            dokkaConfigurationFile.toFile().inputStream(),
        )

        withClue("pluginsClasspath should not contain subproject jars") {
            val pluginClasspathJars = dokkaConfiguration.pluginsClasspath.map { it.name }

            pluginClasspathJars.shouldNotContainAnyOf(
                "subproject-hello.jar",
                "subproject-goodbye.jar",
            )

            pluginClasspathJars.shouldContainExactlyInAnyOrder(
                "markdown-jvm-0.3.1.jar",
                "kotlin-analysis-intellij-1.7.20.jar",
                "dokka-base-1.7.20.jar",
                "templating-plugin-1.7.20.jar",
                "dokka-analysis-1.7.20.jar",
                "kotlin-analysis-compiler-1.7.20.jar",
                "kotlinx-html-jvm-0.8.0.jar",
                "freemarker-2.3.31.jar"
            )
        }
    }
}
