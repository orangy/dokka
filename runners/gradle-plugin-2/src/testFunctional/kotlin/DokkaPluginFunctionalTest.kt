package org.jetbrains.dokka.gradle

import io.kotest.assertions.withClue
import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.gradle.utils.buildGradleKts
import org.jetbrains.dokka.gradle.utils.gradleKtsProjectTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DokkaPluginFunctionalTest {

    @Test
    fun `expect Dokka Plugin creates Dokka tasks`() {
        val build = gradleKtsProjectTest {
            buildGradleKts = """
                plugins {
                    id("org.jetbrains.dokka2") version "2.0.0"
                }
            """.trimIndent()
        }.runner
            .withArguments("tasks")
            .build()

        assertTrue(
            build.output.contains(
                """
                    Dokka tasks
                    -----------
                    createDokkaConfiguration - Assembles Dokka a configuration file, to be used when executing Dokka
                    createDokkaModuleConfiguration
                    dokkaGenerate
                """.trimIndent()
            ),
            "expect output contains dokka tasks\n\n${build.output.prependIndent("  | ")}"
        )
    }

    @Test
    fun `expect Dokka Plugin creates Dokka outgoing variants`() {
        val build = gradleKtsProjectTest {
            buildGradleKts = """
                plugins {
                    id("org.jetbrains.dokka2") version "2.0.0"
                }
            """.trimIndent()
        }.runner
            .withArguments("outgoingVariants")
            .build()

        withClue("dokkaConfigurationElements") {
            build.output shouldContain """
                --------------------------------------------------
                Variant dokkaConfigurationElements
                --------------------------------------------------
                Provide Dokka Configurations files to other subprojects
                
                Capabilities
                    - :test:unspecified (default capability)
                Attributes
                    - org.gradle.category = configuration
                    - org.gradle.usage    = org.jetbrains.dokka
            """.trimIndent()
        }

        withClue("dokkaModuleDescriptorElements") {
            build.output shouldContain """
                --------------------------------------------------
                Variant dokkaModuleDescriptorElements
                --------------------------------------------------
                Provide Dokka module descriptor files to other subprojects
                
                Capabilities
                    - :test:unspecified (default capability)
                Attributes
                    - org.gradle.category = module-descriptor
                    - org.gradle.usage    = org.jetbrains.dokka
            """.trimIndent()
//                Artifacts
//                    - build/dokka/createDokkaModuleConfiguration.json (artifactType = json)
        }
    }

    @Test
    fun `expect Dokka Plugin creates Dokka resolvable configurations`() {
        val build = gradleKtsProjectTest {
            buildGradleKts = """
                plugins {
                    id("org.jetbrains.dokka2") version "2.0.0"
                }
            """.trimIndent()
        }.runner
            .withArguments("resolvableConfigurations")
            .build()

        withClue("Configuration dokka") {
            build.output shouldContain """
                --------------------------------------------------
                Configuration dokka
                --------------------------------------------------
                Fetch all Dokka files from all configurations in other subprojects
                
                Attributes
                    - org.gradle.usage = org.jetbrains.dokka
            """.trimIndent()
        }

        withClue("dokkaConfigurations") {
            build.output shouldContain """
                --------------------------------------------------
                Configuration dokkaConfigurations
                --------------------------------------------------
                Fetch Dokka Configuration files from other subprojects
                
                Attributes
                    - org.gradle.category = configuration
                    - org.gradle.usage    = org.jetbrains.dokka
                Extended Configurations
                    - dokka
            """.trimIndent()
        }

        withClue("dokkaModuleDescriptor") {
            build.output shouldContain """
                --------------------------------------------------
                Configuration dokkaModuleDescriptor
                --------------------------------------------------
                Fetch Dokka module descriptor files from other subprojects
                
                Attributes
                    - org.gradle.category = module-descriptor
                    - org.gradle.usage    = org.jetbrains.dokka
                Extended Configurations
                    - dokka
            """.trimIndent()
        }

        withClue("dokkaPluginsClasspath") {
            build.output shouldContain """
                --------------------------------------------------
                Configuration dokkaPluginsClasspath
                --------------------------------------------------
                Dokka Plugins classpath
                
                Attributes
                    - org.gradle.category                = library
                    - org.gradle.dependency.bundling     = external
                    - org.gradle.jvm.environment         = standard-jvm
                    - org.gradle.libraryelements         = jar
                    - org.jetbrains.kotlin.platform.type = jvm
            """.trimIndent()
        }
    }
}
