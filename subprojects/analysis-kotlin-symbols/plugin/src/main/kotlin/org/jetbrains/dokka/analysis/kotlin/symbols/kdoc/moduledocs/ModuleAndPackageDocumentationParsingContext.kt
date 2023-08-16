package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.logIfNotResolved
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Package
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocTextLink
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.FqName

internal fun interface ModuleAndPackageDocumentationParsingContext {
    fun markdownParserFor(fragment: ModuleAndPackageDocumentationFragment, location: String): MarkdownParser
}

internal fun ModuleAndPackageDocumentationParsingContext.parse(
    fragment: ModuleAndPackageDocumentationFragment
): DocumentationNode {
    return markdownParserFor(fragment, fragment.source.sourceDescription).parse(fragment.documentation)
}

internal fun ModuleAndPackageDocumentationParsingContext(
    logger: DokkaLogger,
    kotlinAnalysis: KotlinAnalysis? = null,
    sourceSet: DokkaConfiguration.DokkaSourceSet? = null
) = ModuleAndPackageDocumentationParsingContext { fragment, sourceLocation ->

    if(kotlinAnalysis == null || sourceSet == null) {
        MarkdownParser(externalDri = { null }, sourceLocation)
    } else {
        val analysisContext = kotlinAnalysis[sourceSet]
        analyze(analysisContext.mainModule) {
            val contextSymbol = when (fragment.classifier) {
                Module -> ROOT_PACKAGE_SYMBOL
                Package -> getPackageSymbolIfPackageExists(FqName(fragment.name))
            }

            MarkdownParser(
                externalDri = { resolveKDocTextLink(it, contextSymbol?.psi).logIfNotResolved(it, logger) },
                sourceLocation
            )
        }
    }
}
