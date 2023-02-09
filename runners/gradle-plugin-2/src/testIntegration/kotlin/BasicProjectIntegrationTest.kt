package org.jetbrains.dokka.gradle.it

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.gradle.dokka_configuration.DokkaConfigurationKxs
import org.jetbrains.dokka.gradle.utils.gradleKtsProjectIntegrationTest
import org.jetbrains.dokka.gradle.utils.gradleProperties
import org.jetbrains.dokka.gradle.utils.parseJson
import org.junit.jupiter.api.Test

class BasicProjectIntegrationTest {

    private val basicProject = gradleKtsProjectIntegrationTest("it-basic") {
        gradleProperties = gradleProperties.lines().joinToString("\n") { line ->
            if (line.startsWith("testMavenRepoDir")) {
                "testMavenRepoDir=${testMavenRepoRelativePath}"
            } else {
                line
            }
        }
    }

    @Test
    fun `test basic project`() {
        val build = basicProject.runner
            .withArguments(
                "clean",
                "dokkaGenerate",
                "--stacktrace",
                "--info",
            )
            .forwardOutput()
            .build()

        build.output shouldContain "BUILD SUCCESSFUL"
        build.output shouldContain "Generation completed successfully"


        val actualDokkaConfJson = basicProject
            .projectDir
            .resolve("build/dokka-config/html/dokka_configuration.json")
            .toFile()
            .readText()

        val actualDokkaConf: DokkaConfiguration =
            Json.decodeFromString(DokkaConfigurationKxs.serializer(), actualDokkaConfJson)

        actualDokkaConf.moduleName shouldBeEqualComparingTo expectedDokkaConf.moduleName
        if (expectedDokkaConf.moduleVersion == null) {
            actualDokkaConf.moduleVersion shouldBe null
        } else {
            actualDokkaConf.moduleVersion shouldBe expectedDokkaConf.moduleVersion
        }

        // TODO compare output dir...
        //      This might need a hefty refactor. Currently there's only one Dokka Generator execution, so
        //      there's only one output dir. We need a Dokka Generator per Dokka format.
        //        - create a DomainObject for each Dokka Generator execution
        //        - each DomainObject should have its own configurations, plugins, etc...
        //        - for each DomainObject, create Dokka tasks
        // actualDokkaConf.outputDir shouldBeEqualComparingTo expectedDokkaConf.outputDir

        if (expectedDokkaConf.cacheRoot == null) {
            actualDokkaConf.cacheRoot shouldBe null
        } else {
            actualDokkaConf.cacheRoot shouldBe expectedDokkaConf.cacheRoot
        }

        actualDokkaConf.offlineMode shouldBeEqualComparingTo expectedDokkaConf.offlineMode
        actualDokkaConf.failOnWarning shouldBeEqualComparingTo expectedDokkaConf.failOnWarning

        withClue("comparing source sets") {
            // TODO compare source sets
            actualDokkaConf.sourceSets shouldBeSameSizeAs expectedDokkaConf.sourceSets
        }

        withClue("comparing modules") {
            // TODO compare modules
            actualDokkaConf.modules shouldBeSameSizeAs expectedDokkaConf.modules
        }

        withClue("comparing plugins classpath") {
            // TODO compare plugins classpath
//            actualDokkaConf.pluginsClasspath shouldBeSameSizeAs expectedDokkaConf.pluginsClasspath

            actualDokkaConf.pluginsClasspath.map { it.name }.shouldContainExactlyInAnyOrder(
                "markdown-jvm-0.3.1.jar",
                "kotlin-analysis-intellij-1.7.20.jar",
                "dokka-base-1.7.20.jar",
                "templating-plugin-1.7.20.jar",
                "dokka-analysis-1.7.20.jar",
                "kotlin-analysis-compiler-1.7.20.jar",
                "kotlinx-html-jvm-0.8.0.jar",
                "freemarker-2.3.31.jar",
            )
        }

        withClue("comparing plugins configurations") {
            // TODO compare plugins configuration
            actualDokkaConf.pluginsConfiguration shouldBeSameSizeAs expectedDokkaConf.pluginsConfiguration
        }

        withClue("comparing delayTemplateSubstitution") {
            actualDokkaConf.delayTemplateSubstitution shouldBeEqualComparingTo expectedDokkaConf.delayTemplateSubstitution
        }
        withClue("comparing suppressObviousFunctions") {
            actualDokkaConf.suppressObviousFunctions shouldBeEqualComparingTo expectedDokkaConf.suppressObviousFunctions
        }
        withClue("comparing includes") {
            // TODO compare includes
            actualDokkaConf.includes shouldBeSameSizeAs expectedDokkaConf.includes
        }
        withClue("comparing suppressInheritedMembers") {
            actualDokkaConf.suppressInheritedMembers shouldBeEqualComparingTo expectedDokkaConf.suppressInheritedMembers
        }
        withClue("comparing finalizeCoroutines") {
            actualDokkaConf.finalizeCoroutines shouldBeEqualComparingTo false // expectedDokkaConf.finalizeCoroutines
        }
    }
}

private val expectedDokkaConf: DokkaConfiguration = parseJson<DokkaConfigurationImpl>(
// language=json
    """
{
  "moduleName": "Basic Project",
  "moduleVersion": "1.7.20-SNAPSHOT",
  "outputDir": ".../build/dokka/html",
  "cacheRoot": null,
  "offlineMode": false,
  "sourceSets": [
    {
      "displayName": "jvm",
      "sourceSetID": {
        "scopeId": ":dokkaHtml",
        "sourceSetName": "main"
      },
      "classpath": [
        ".../kotlin-stdlib-1.7.20.jar",
        ".../kotlin-stdlib-common-1.7.20.jar",
        ".../annotations-13.0.jar"
      ],
      "sourceRoots": [
        ".../src/main/kotlin",
        ".../src/main/java"
      ],
      "dependentSourceSets": [],
      "samples": [],
      "includes": [],
      "includeNonPublic": false,
      "reportUndocumented": false,
      "skipEmptyPackages": true,
      "skipDeprecated": false,
      "jdkVersion": 8,
      "sourceLinks": [
        {
          "localDirectory": ".../src/main",
          "remoteUrl": "https://github.com/Kotlin/dokka/tree/master/integration-tests/gradle/projects/it-basic/src/main",
          "remoteLineSuffix": "#L"
        }
      ],
      "perPackageOptions": [
        {
          "matchingRegex": "it.suppressedByPackage.*",
          "includeNonPublic": false,
          "reportUndocumented": false,
          "skipDeprecated": false,
          "suppress": true,
          "documentedVisibilities": [
            "PUBLIC"
          ]
        },
        {
          "matchingRegex": "it.overriddenVisibility.*",
          "includeNonPublic": false,
          "reportUndocumented": false,
          "skipDeprecated": false,
          "suppress": false,
          "documentedVisibilities": [
            "PRIVATE"
          ]
        }
      ],
      "externalDocumentationLinks": [
        {
          "url": "https://docs.oracle.com/javase/8/docs/api/",
          "packageListUrl": "https://docs.oracle.com/javase/8/docs/api/package-list"
        },
        {
          "url": "https://kotlinlang.org/api/latest/jvm/stdlib/",
          "packageListUrl": "https://kotlinlang.org/api/latest/jvm/stdlib/package-list"
        }
      ],
      "languageVersion": null,
      "apiVersion": null,
      "noStdlibLink": false,
      "noJdkLink": false,
      "suppressedFiles": [
        ".../src/main/kotlin/it/suppressedByPath"
      ],
      "analysisPlatform": "jvm",
      "documentedVisibilities": [
        "PUBLIC",
        "PROTECTED"
      ]
    }
  ],
  "pluginsClasspath": [
    ".../dokka-analysis-1.8.0-SNAPSHOT.jar",
    ".../dokka-base-1.8.0-SNAPSHOT.jar",
    ".../kotlin-analysis-intellij-1.8.0-SNAPSHOT.jar",
    ".../kotlin-analysis-compiler-1.8.0-SNAPSHOT.jar",
    ".../kotlinx-html-jvm-0.7.5.jar",
    ".../kotlinx-coroutines-core-jvm-1.6.3.jar",
    ".../kotlin-stdlib-jdk8-1.7.20.jar",
    ".../jackson-databind-2.12.7.1.jar",
    ".../jackson-annotations-2.12.7.jar",
    ".../jackson-core-2.12.7.jar",
    ".../jackson-module-kotlin-2.12.7.jar",
    ".../kotlin-reflect-1.7.20.jar",
    ".../kotlin-stdlib-jdk7-1.7.20.jar",
    ".../kotlin-stdlib-1.7.20.jar",
    ".../jsoup-1.15.3.jar",
    ".../freemarker-2.3.31.jar",
    ".../kotlin-stdlib-common-1.7.20.jar",
    ".../annotations-13.0.jar"
  ],
  "pluginsConfiguration": [
    {
      "fqPluginName": "org.jetbrains.dokka.base.DokkaBase",
      "serializationFormat": "JSON",
      "values": "{ \"customStyleSheets\": [\".../customResources/logo-styles.css\", \".../customResources/custom-style-to-add.css\"], \"customAssets\" : [\".../customResources/custom-resource.svg\"] }"
    }
  ],
  "modules": [],
  "failOnWarning": false,
  "delayTemplateSubstitution": false,
  "suppressObviousFunctions": false,
  "includes": [],
  "suppressInheritedMembers": false,
  "finalizeCoroutines": true
}
""".trimIndent()
)
