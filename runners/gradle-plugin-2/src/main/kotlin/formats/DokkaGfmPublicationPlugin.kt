package org.jetbrains.dokka.gradle.formats

import org.gradle.kotlin.dsl.dependencies

abstract class DokkaGfmPublicationPlugin : DokkaPublicationBasePlugin(
    formatName = "gfm"
) {
    override fun PublicationPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("gfm-plugin"))
        }
    }
}
