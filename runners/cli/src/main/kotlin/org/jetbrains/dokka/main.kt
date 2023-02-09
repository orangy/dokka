package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import org.jetbrains.dokka.utilities.*
import java.nio.file.Paths

fun main(args: Array<String>) {
    val globalArguments = GlobalArguments(args)
    val configuration = initializeConfiguration(globalArguments)
    DokkaGenerator(configuration, globalArguments.logger).generate()
}

fun initializeConfiguration(globalArguments: GlobalArguments): DokkaConfiguration {
    return if (globalArguments.json != null) {
        val jsonContent = Paths.get(checkNotNull(globalArguments.json)).toFile().readText()
        val globals = GlobalDokkaConfiguration(jsonContent)
        val dokkaConfigurationImpl = DokkaConfigurationImpl(jsonContent)

        dokkaConfigurationImpl.apply(globals).apply {
            sourceSets.forEach {
                it.externalDocumentationLinks.cast<MutableSet<ExternalDocumentationLink>>().addAll(defaultLinks(it))
            }
        }
    } else {
        globalArguments
    }
}
