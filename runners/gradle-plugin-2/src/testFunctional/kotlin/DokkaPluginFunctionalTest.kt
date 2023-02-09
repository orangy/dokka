package org.jetbrains.dokka.gradle

import io.kotest.assertions.withClue
import io.kotest.matchers.string.shouldContain
import org.jetbrains.dokka.gradle.utils.buildGradleKts
import org.jetbrains.dokka.gradle.utils.gradleKtsProjectTest
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

        build.output shouldContain /* language=text */ """
            |Dokka tasks
            |-----------
            |createDokkaConfiguration - Runs all Dokka Create Configuration tasks
            |createDokkaConfigurationGfm - Creates Dokka Configuration for executing the Dokka Generator for the gfm publication
            |createDokkaConfigurationHtml - Creates Dokka Configuration for executing the Dokka Generator for the html publication
            |createDokkaConfigurationJavadoc - Creates Dokka Configuration for executing the Dokka Generator for the javadoc publication
            |createDokkaConfigurationJekyll - Creates Dokka Configuration for executing the Dokka Generator for the jekyll publication
            |dokkaGenerate - Runs all Dokka Generate tasks
            |dokkaGenerateGfm - Executes the Dokka Generator, producing the gfm publication
            |dokkaGenerateHtml - Executes the Dokka Generator, producing the html publication
            |dokkaGenerateJavadoc - Executes the Dokka Generator, producing the javadoc publication
            |dokkaGenerateJekyll - Executes the Dokka Generator, producing the jekyll publication
            |
        """.trimMargin()
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


        fun checkVariant(format: String) {
            val formatCapitalized = format.capitalize()

            build.output shouldContain /* language=text */ """
                |--------------------------------------------------
                |Variant dokkaConfigurationElements$formatCapitalized
                |--------------------------------------------------
                |Provide Dokka Generator Configuration files for $format to other subprojects
                |
                |Capabilities
                |    - :test:unspecified (default capability)
                |Attributes
                |    - org.jetbrains.dokka.base     = dokka
                |    - org.jetbrains.dokka.category = configuration
                |    - org.jetbrains.dokka.format   = $format
                |Artifacts
                |    - build/dokka-config/$format/dokka_configuration.json (artifactType = json)
                |
            """.trimMargin()
        }

        checkVariant("gfm")
        checkVariant("html")
        checkVariant("javadoc")
        checkVariant("jekyll")
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
            build.output shouldContain /* language=text */ """
                |--------------------------------------------------
                |Configuration dokka
                |--------------------------------------------------
                |Fetch all Dokka files from all configurations in other subprojects
                |
                |Attributes
                |    - org.jetbrains.dokka.base = dokka
            """.trimMargin()
        }

        fun checkConfigurations(format: String) {
            val formatCapitalized = format.capitalize()

            build.output shouldContain /* language=text */ """
                |--------------------------------------------------
                |Configuration dokkaConfiguration$formatCapitalized
                |--------------------------------------------------
                |Fetch Dokka Generator Configuration files for $format from other subprojects
                |
                |Attributes
                |    - org.jetbrains.dokka.base     = dokka
                |    - org.jetbrains.dokka.category = configuration
                |    - org.jetbrains.dokka.format   = $format
                |Extended Configurations
                |    - dokka
            """.trimMargin()


            build.output shouldContain /* language=text */ """
                |--------------------------------------------------
                |Configuration dokkaGeneratorClasspath$formatCapitalized
                |--------------------------------------------------
                |Dokka Generator runtime classpath for $format - will be used in Dokka Worker. Should contain all transitive dependencies, plugins (and their transitive dependencies), so Dokka Worker can run.
                |
                |Attributes
                |    - org.gradle.category            = library
                |    - org.gradle.dependency.bundling = external
                |    - org.gradle.jvm.environment     = standard-jvm
                |    - org.gradle.libraryelements     = jar
                |    - org.gradle.usage               = java-runtime
                |    - org.jetbrains.dokka.base       = dokka
                |    - org.jetbrains.dokka.category   = generator-classpath
                |    - org.jetbrains.dokka.format     = $format
                |Extended Configurations
                |    - dokkaPlugin$formatCapitalized
            """.trimMargin()

            build.output shouldContain /* language=text */ """
                |--------------------------------------------------
                |Configuration dokkaPlugin$formatCapitalized
                |--------------------------------------------------
                |Dokka Plugins classpath for $format
                |
                |Attributes
                |    - org.gradle.category            = library
                |    - org.gradle.dependency.bundling = external
                |    - org.gradle.jvm.environment     = standard-jvm
                |    - org.gradle.libraryelements     = jar
                |    - org.gradle.usage               = java-runtime
                |    - org.jetbrains.dokka.base       = dokka
                |    - org.jetbrains.dokka.category   = plugins-classpath
                |    - org.jetbrains.dokka.format     = $format
            """.trimMargin()

            build.output shouldContain /* language=text */ """
                |--------------------------------------------------
                |Configuration dokkaPluginIntransitive$formatCapitalized
                |--------------------------------------------------
                |Dokka Plugins classpath for $format - for internal use. Fetch only the plugins (no transitive dependencies) for use in the Dokka JSON Configuration.
                |
                |Attributes
                |    - org.gradle.category            = library
                |    - org.gradle.dependency.bundling = external
                |    - org.gradle.jvm.environment     = standard-jvm
                |    - org.gradle.libraryelements     = jar
                |    - org.gradle.usage               = java-runtime
                |    - org.jetbrains.dokka.base       = dokka
                |    - org.jetbrains.dokka.category   = plugins-classpath
                |    - org.jetbrains.dokka.format     = $format
                |Extended Configurations
                |    - dokkaPlugin$formatCapitalized
            """.trimMargin()
        }

        checkConfigurations("gfm")
        checkConfigurations("html")
        checkConfigurations("javadoc")
        checkConfigurations("jekyll")
    }
}

/*

--------------------------------------------------
Configuration dokkaConfigurationGfm
--------------------------------------------------
Fetch Dokka Generator Configuration files for gfm from other subprojects

Attributes
    - org.jetbrains.dokka.base     = dokka
    - org.jetbrains.dokka.category = configuration
    - org.jetbrains.dokka.format   = gfm
Extended Configurations
    - dokka

--------------------------------------------------
Configuration dokkaConfigurationGfm
--------------------------------------------------
Fetch Dokka Generator Configuration files for gfm from other subprojects

Attributes
    - org.jetbrains.dokka.base     = dokka
    - org.jetbrains.dokka.category = configuration
    - org.jetbrains.dokka.format   = gfm
Extended Configurations
    - dokka
 */
