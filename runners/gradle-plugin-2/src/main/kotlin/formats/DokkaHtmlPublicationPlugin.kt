package org.jetbrains.dokka.gradle.formats

abstract class DokkaHtmlPublicationPlugin : DokkaPublicationBasePlugin(
    formatName = "html"
) {
    // HTML is the default - no special config needed!
}
