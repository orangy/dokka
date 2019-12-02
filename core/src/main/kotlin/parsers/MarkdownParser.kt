package parsers

import model.doc.*
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.parsers.factories.DocNodesFromIElementFactory
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.intellij.markdown.parser.MarkdownParser as IntellijMarkdownParser

class MarkdownParser (
    private val resolutionFacade: DokkaResolutionFacade,
    private val declarationDescriptor: DeclarationDescriptor
    ) : Parser() {

    inner class MarkdownVisitor(val text: String) {

        private fun headersHandler(node: ASTNode): DocNode =
            DocNodesFromIElementFactory.getInstance(node.type, visitNode(node.children.find { it.type == MarkdownTokenTypes.ATX_CONTENT }!!).children.drop(1))

        private fun horizontalRulesHandler(node: ASTNode): DocNode =
            DocNodesFromIElementFactory.getInstance(MarkdownTokenTypes.HORIZONTAL_RULE)

        private fun emphasisHandler(node: ASTNode): DocNode =
            DocNodesFromIElementFactory.getInstance(node.type, children = listOf(visitNode(node.children[node.children.size/2])))

        private fun blockquotesHandler(node: ASTNode): DocNode =
            DocNodesFromIElementFactory.getInstance(node.type, children = node.children.drop(1).map { visitNode(it) })

        private fun listsHandler(node: ASTNode): DocNode =
            DocNodesFromIElementFactory.getInstance(
                node.type,
                children = node
                    .children
                    .filter { it.type == MarkdownElementTypes.LIST_ITEM }
                    .map {
                        DocNodesFromIElementFactory.getInstance(
                            it.type,
                            children = it
                                .children
                                .drop(1)
                                .map { visitNode(it) }
                        )
                    },
                params =
                        if(node.type == MarkdownElementTypes.ORDERED_LIST) {
                            val listNumberNode = node.children.first().children.first()
                            mapOf("start" to text.substring(listNumberNode.startOffset, listNumberNode.endOffset).dropLast(2))
                        }
                        else
                            emptyMap()
            )

        private fun linksHandler(node: ASTNode): DocNode {
            val linkNode = node.children.find { it.type == MarkdownElementTypes.LINK_LABEL }!!.children[1]
            val link = text.substring(linkNode.startOffset, linkNode.endOffset)

            val dri: DRI? = if (link.startsWith("http") || link.startsWith("www")) {
                null
            } else {
                DRI.from(
                        resolveKDocLink(
                        resolutionFacade.resolveSession.bindingContext,
                        resolutionFacade,
                        declarationDescriptor,
                        null,
                        link.split('.')
                    ).single()
                )
            }

            val href = mapOf("href" to link)
            return when (node.type) {
                MarkdownElementTypes.FULL_REFERENCE_LINK -> DocNodesFromIElementFactory.getInstance(node.type, params = href, children = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }!!.children.drop(1).dropLast(1).map { visitNode(it) }, dri = dri)
                else                                     -> DocNodesFromIElementFactory.getInstance(node.type, params = href, children = listOf(visitNode(linkNode)), dri = dri)
            }
        }

        private fun imagesHandler(node: ASTNode): DocNode {
            val linkNode = node.children.last().children.find { it.type == MarkdownElementTypes.LINK_LABEL }!!.children[1]
            val link = text.substring(linkNode.startOffset, linkNode.endOffset)
            val src = mapOf("src" to link)
            return DocNodesFromIElementFactory.getInstance(node.type, params = src, children = listOf(visitNode(node.children.last().children.find { it.type == MarkdownElementTypes.LINK_TEXT }!!)))
        }

        private fun codeSpansAndFencesHandler(node: ASTNode): DocNode =
            DocNodesFromIElementFactory.getInstance(node.type, children = node.children.drop(1).dropLast(1).map { visitNode(it) })

        private fun codeBlocksHandler(node: ASTNode): DocNode =
            DocNodesFromIElementFactory.getInstance(node.type, children = node.children.map { visitNode(it) })

        private fun defaultHandler(node: ASTNode): DocNode =
            DocNodesFromIElementFactory.getInstance(MarkdownElementTypes.PARAGRAPH, children = node.children.map { visitNode(it) })

        fun visitNode(node: ASTNode): DocNode =
            when (node.type) {
                MarkdownElementTypes.ATX_1,
                MarkdownElementTypes.ATX_2,
                MarkdownElementTypes.ATX_3,
                MarkdownElementTypes.ATX_4,
                MarkdownElementTypes.ATX_5,
                MarkdownElementTypes.ATX_6                  -> headersHandler(node)
                MarkdownTokenTypes.HORIZONTAL_RULE          -> horizontalRulesHandler(node)
                MarkdownElementTypes.STRONG,
                MarkdownElementTypes.EMPH                   -> emphasisHandler(node)
                MarkdownElementTypes.FULL_REFERENCE_LINK,
                MarkdownElementTypes.SHORT_REFERENCE_LINK   -> linksHandler(node)
                MarkdownElementTypes.BLOCK_QUOTE            -> blockquotesHandler(node)
                MarkdownElementTypes.UNORDERED_LIST,
                MarkdownElementTypes.ORDERED_LIST           -> listsHandler(node)
                MarkdownElementTypes.CODE_BLOCK             -> codeBlocksHandler(node)
                MarkdownElementTypes.CODE_FENCE,
                MarkdownElementTypes.CODE_SPAN              -> codeSpansAndFencesHandler(node)
                MarkdownElementTypes.IMAGE                  -> imagesHandler(node)
                MarkdownTokenTypes.EOL                      -> DocNodesFromIElementFactory.getInstance(MarkdownTokenTypes.TEXT, body = "\n")
                MarkdownTokenTypes.WHITE_SPACE              -> DocNodesFromIElementFactory.getInstance(MarkdownTokenTypes.TEXT, body = " ")
                MarkdownTokenTypes.CODE_FENCE_CONTENT,
                MarkdownTokenTypes.CODE_LINE,
                MarkdownTokenTypes.TEXT                     -> DocNodesFromIElementFactory.getInstance(MarkdownTokenTypes.TEXT, body = text.substring(node.startOffset, node.endOffset))
                else                                        -> defaultHandler(node)
            }
    }

    private fun markdownToDocNode(text: String): DocNode {

        val flavourDescriptor = CommonMarkFlavourDescriptor()
        val markdownAstRoot: ASTNode = IntellijMarkdownParser(flavourDescriptor).buildMarkdownTreeFromString(text)

        return MarkdownVisitor(text).visitNode(markdownAstRoot)
    }

    override fun parseStringToDocNode(extractedString: String) = markdownToDocNode(extractedString)
    override fun preparse(text: String) = text

    fun parseFromKDocTag(kDocTag: KDocTag?): DocHeader {
        val test = if(kDocTag == null)
            DocHeader(emptyList())
        else
            DocHeader(
                (listOf(kDocTag) + kDocTag.children).filter { it is KDocTag }.map {
                    when( (it as KDocTag).knownTag ) {
                        null                        -> Description(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.AUTHOR         -> Author(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.THROWS         -> Throws(parseStringToDocNode(it.getContent()), it.getSubjectName()!!)
                        KDocKnownTag.EXCEPTION      -> Throws(parseStringToDocNode(it.getContent()), it.getSubjectName()!!)
                        KDocKnownTag.PARAM          -> Param(parseStringToDocNode(it.getContent()), it.getSubjectName()!!)
                        KDocKnownTag.RECEIVER       -> Receiver(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.RETURN         -> Return(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.SEE            -> See(parseStringToDocNode(it.getContent()), it.getSubjectName()!!)
                        KDocKnownTag.SINCE          -> Since(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.CONSTRUCTOR    -> Constructor(parseStringToDocNode(it.getContent()))
                        KDocKnownTag.PROPERTY       -> Property(parseStringToDocNode(it.getContent()), it.getSubjectName()!!)
                        KDocKnownTag.SAMPLE         -> Sample(parseStringToDocNode(it.getContent()), it.getSubjectName()!!)
                        KDocKnownTag.SUPPRESS       -> Suppress(parseStringToDocNode(it.getContent()))
                    }
                }
            )
        return test
    }




}