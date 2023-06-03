package renderers

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel

fun testPage(callback: PageContentBuilder.DocumentableContentBuilder.() -> Unit): RawTestPage {
    val content = PageContentBuilder(
        EmptyCommentConverter,
        KotlinSignatureProvider(EmptyCommentConverter, DokkaConsoleLogger(LoggingLevel.DEBUG)),
        DokkaConsoleLogger(LoggingLevel.DEBUG)
    ).contentFor(
        DRI.topLevel,
        emptySet(),
        block = callback
    )

    return RawTestPage(content)
}

class RawTestPage(
    override val content: ContentNode,
    override val name: String = "testPage",
    override val dri: Set<DRI> = setOf(DRI.topLevel),
    override val embeddedResources: List<String> = emptyList(),
    override val children: List<PageNode> = emptyList(),
): RootPageNode(), ContentPage {
    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = this

    override fun modified(name: String, children: List<PageNode>): RootPageNode = this

}

internal object EmptyCommentConverter : CommentsToContentConverter {
    override fun buildContent(
        docTag: DocTag,
        dci: DCI,
        sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
        styles: Set<Style>,
        extras: PropertyContainer<ContentNode>
    ): List<ContentNode> = emptyList()
}
