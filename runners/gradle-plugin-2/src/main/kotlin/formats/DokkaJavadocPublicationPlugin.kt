package org.jetbrains.dokka.gradle.formats

import org.gradle.kotlin.dsl.dependencies

abstract class DokkaJavadocPublicationPlugin : DokkaPublicationBasePlugin(
    formatName = "javadoc"
) {
    override fun PublicationPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("javadoc-plugin"))
        }
    }
}
