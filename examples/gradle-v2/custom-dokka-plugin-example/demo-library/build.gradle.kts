import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.dokka") version "2.0.20-SNAPSHOT"
}

dependencies {
    dokkaPlugin(project(":dokka-plugin-hide-internal-api"))
}

dokka {
    moduleName.set("Demo Library")

    pluginsConfiguration {
        registerBinding(HideInternalApiParameters::class, HideInternalApiParameters::class)
        register<HideInternalApiParameters>("HideInternalApiPlugin") {
            annotatedWith.add("demo.HideFromDokka")
        }
    }
}

/**
 * Define custom parameters for the HideInternalApi Dokka Plugin.
 *
 * Using a custom class for defining parameters means Gradle can accurately check for task up-to-date checks.
 *
 * If you find you need to re-use the plugin parameters configuration class in multiple buildscripts,
 * move the class into a shared location, like `buildSrc` (or another included-build for build conventions).
 * See [Sharing Build Logic between Subprojects](https://docs.gradle.org/8.10/userguide/sharing_build_logic_between_subprojects.html).
 */
@OptIn(DokkaInternalApi::class)
abstract class HideInternalApiParameters @Inject constructor(
    name: String
) : DokkaPluginParametersBaseSpec(name, "demo.dokka.plugin.HideInternalApiPlugin") {

    @get:Input
    @get:Optional
    abstract val annotatedWith: ListProperty<String>

    override fun jsonEncode(): String {
        // Convert annotatedWith to a JSON list.
        val annotatedWithJson = annotatedWith.orNull.orEmpty()
            .joinToString(separator = ", ", prefix = "[", postfix = "]") { "\"$it\"" }

        return """
            {
              "annotatedWith": $annotatedWithJson
            }
            """.trimIndent()
    }
}
