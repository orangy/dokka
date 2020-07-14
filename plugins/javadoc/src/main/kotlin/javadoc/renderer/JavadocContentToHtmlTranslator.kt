package javadoc.renderer

import javadoc.location.JavadocLocationProvider
import javadoc.pages.JavadocSignatureContentNode
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.htmlEscape

internal class JavadocContentToHtmlTranslator(
    private val locationProvider: JavadocLocationProvider,
    private val context: DokkaContext
) {

    fun htmlForContentNode(node: ContentNode, relative: PageNode?): String =
        when (node) {
            is ContentGroup -> htmlForContentNodes(node.children, relative)
            is ContentText -> node.text.htmlEscape()
            is ContentDRILink -> buildLink(
                locationProvider.resolve(node.address, node.sourceSets, relative),
                htmlForContentNodes(node.children, relative)
            )
            is ContentResolvedLink -> buildLink(node.address, htmlForContentNodes(node.children, relative))
            is ContentCode -> htmlForCode(node.children)
            is JavadocSignatureContentNode -> htmlForSignature(node, relative)
            else -> ""
        }

    fun htmlForContentNodes(list: List<ContentNode>, relative: PageNode?) =
        list.joinToString(separator = "") { htmlForContentNode(it, relative) }

    private fun htmlForCode(code: List<ContentNode>): String = code.map { element ->
        when (element) {
            is ContentText -> element.text
            is ContentBreakLine -> ""
            else -> run { context.logger.error("Cannot cast $element as ContentText!"); "" }
        }
    }.joinToString("<br>", """<span class="code">""", "</span>") { it }

    private fun htmlForSignature(node: JavadocSignatureContentNode, relative: PageNode?): String =
        listOfNotNull(
            node.annotations,
            node.modifiers,
            node.signatureWithoutModifiers,
            node.supertypes
        ).joinToString(separator = " ") { htmlForContentNode(it, relative) }

    companion object {

        fun buildLink(address: String, content: String) =
            """<a href=${address.formatToEndWithHtml()}>$content</a>"""

        private fun String.formatToEndWithHtml() =
            if (endsWith(".html") || contains(Regex("\\.html#"))) this else "$this.html"
    }
}