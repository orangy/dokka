package org.jetbrains.dokka.jekyll

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.single
import org.jetbrains.dokka.renderers.DefaultRenderer
import org.jetbrains.dokka.renderers.OutputWriter
import org.jetbrains.dokka.resolvers.DefaultLocationProvider
import org.jetbrains.dokka.resolvers.LocationProvider
import org.jetbrains.dokka.resolvers.LocationProviderFactory
import java.lang.StringBuilder


class JekyllPlugin : DokkaPlugin() {

    val renderer by extending {
        CoreExtensions.renderer providing { JekyllRenderer(it.single(CoreExtensions.outputWriter), it) }
    }
}

class JekyllRenderer(
    outputWriter: OutputWriter,
    context: DokkaContext
) : DefaultRenderer<StringBuilder>(outputWriter, context) {
    override fun StringBuilder.buildHeader(level: Int, content: StringBuilder.() -> Unit) {
        buildParagraph()
        append("${"#".repeat(level)} ")
        content()
        buildNewLine()
    }

    override fun StringBuilder.buildLink(address: String, content: StringBuilder.() -> Unit) {
        append("[")
        content()
        append("]($address)")
    }

    override fun StringBuilder.buildList(node: ContentList, pageContext: ContentPage) {
        buildListLevel(node, pageContext)
        buildParagraph()
    }

    private val indent = " ".repeat(4)

    private fun StringBuilder.buildListItem(items: List<ContentNode>, pageContext: ContentPage, bullet: String = "*") {
        items.forEach {
            if(it is ContentList) {
                val builder = StringBuilder()
                builder.append(indent)
                builder.buildListLevel(it, pageContext)
                append(builder.toString().replace(Regex("  \n(?!$)"), "  \n$indent"))
            } else {
                append("$bullet ")
                it.build(this, pageContext)
                buildNewLine()
            }
        }
    }

    private fun StringBuilder.buildListLevel(node: ContentList, pageContext: ContentPage) {
        if(node.ordered) {
            buildListItem(node.children, pageContext, "${node.start}.")
        } else {
            buildListItem(node.children, pageContext, "*")
        }
    }

    override fun StringBuilder.buildNewLine() {
        this.append("  \n")
    }

    fun StringBuilder.buildParagraph() {
        this.append("\n\n")
    }

    override fun StringBuilder.buildResource(node: ContentEmbeddedResource, pageContext: ContentPage) {
        this.append("Resource")
    }

    override fun StringBuilder.buildTable(node: ContentTable, pageContext: ContentPage) {

        val size = node.children.firstOrNull()?.children?.size ?: 0

        if(node.header.size > 0) {
            node.header.forEach {
                it.children.forEach {
                    append("| ")
                    it.build(this, pageContext)
                }
                append("|\n")
            }
        } else {
            append("| ".repeat(size))
            if(size > 0) append("|\n")
        }

        append("|---".repeat(size))
        if(size > 0) append("|\n")


        node.children.forEach {
            it.children.forEach {
                append("| ")
                it.build(this,  pageContext)
            }
            append("|\n")
        }
    }

    override fun StringBuilder.buildText(textNode: ContentText) {
        val decorators = decorators(textNode.style)
        this.append(decorators)
        this.append(textNode.text)
        this.append(decorators.reversed())
    }

    override fun StringBuilder.buildNavigation(page: PageNode) {
        locationProvider.ancestors(page).asReversed().forEach { node ->
            append("/")
            if (node.isNavigable) buildLink(node, page)
            else append(node.name)
        }
        buildParagraph()
    }

    override fun buildPage(page: ContentPage, content: (StringBuilder, ContentPage) -> Unit): String {
        val builder = StringBuilder()
        builder.append("---\n")
        builder.append("title: ${page.name} -\n")
        builder.append("---\n")
        content(builder, page)
        return builder.toString()
    }

    override fun buildError(node: ContentNode) {
        println("Error")
    }

    private fun decorators(styles: Set<Style>): String {
        val decorators = StringBuilder()
        styles.forEach {
            when(it) {
                TextStyle.Bold          -> decorators.append("**")
                TextStyle.Italic        -> decorators.append("*")
                TextStyle.Strong        -> decorators.append("**")
                TextStyle.Strikethrough -> decorators.append("~~")
                else                    -> Unit
            }
        }
        return decorators.toString()
    }

    private val PageNode.isNavigable: Boolean
        get() = this !is RendererSpecificPage || strategy != RenderingStrategy.DoNothing

    private fun StringBuilder.buildLink(to: PageNode, from: PageNode) =
        buildLink(locationProvider.resolve(to, from)) {
            append(to.name)
        }

    override fun renderPage(page: PageNode) {
        val path by lazy { locationProvider.resolve(page, skipExtension = true) }
        when (page) {
            is ContentPage -> outputWriter.write(path, buildPage(page) { c, p -> buildPageContent(c, p) }, ".md")
            is RendererSpecificPage -> when (val strategy = page.strategy) {
                is RenderingStrategy.Copy -> outputWriter.writeResources(strategy.from, path)
                is RenderingStrategy.Write -> outputWriter.write(path, strategy.text, "")
                is RenderingStrategy.Callback -> outputWriter.write(path, strategy.instructions(this, page), ".md")
                RenderingStrategy.DoNothing -> Unit
            }
            else -> throw AssertionError(
                "Page ${page.name} cannot be rendered by renderer as it is not renderer specific nor contains content"
            )
        }
    }
}