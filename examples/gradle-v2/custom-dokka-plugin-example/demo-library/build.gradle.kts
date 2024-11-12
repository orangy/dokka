import org.jetbrains.dokka.gradle.engine.plugins.DokkaPluginParametersBaseSpec
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi

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

        // Register the configuration class with Dokka Gradle Plugin
        registerBinding(HideInternalApiParameters::class, HideInternalApiParameters::class)

        // Configure the custom plugin:
        register<HideInternalApiParameters>("HideInternalApiPlugin") {
            // Tell the plugin to hide code annotated with `@HideFromDokka`.
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
@OptIn(InternalDokkaGradlePluginApi::class)
abstract class HideInternalApiParameters @Inject constructor(
    name: String
) : DokkaPluginParametersBaseSpec(
    name,
    // The plugin ID of the custom HideInternalApiPlugin
    "demo.dokka.plugin.HideInternalApiPlugin",
) {

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
