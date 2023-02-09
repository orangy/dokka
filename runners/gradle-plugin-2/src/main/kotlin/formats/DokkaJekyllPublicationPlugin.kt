package org.jetbrains.dokka.gradle.formats

import org.gradle.kotlin.dsl.dependencies

abstract class DokkaJekyllPublicationPlugin : DokkaPublicationBasePlugin(
    formatName = "jekyll"
) {
    override fun PublicationPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("jekyll-plugin"))
        }
    }
}
