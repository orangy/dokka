@file:Suppress("PackageDirectoryMismatch")

package  org.jetbrains.dokka.test.tools.matchers.content

import org.jetbrains.dokka.model.asPrintableTree
import org.jetbrains.dokka.pages.ContentComposite
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentText
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.safeCast

sealed class MatcherElement

class TextMatcher(val text: String) : MatcherElement()

open class NodeMatcher<T : ContentNode>(
    val kclass: KClass<T>,
    val assertions: T.() -> Unit = {}
) : MatcherElement() {
    open fun tryMatch(node: ContentNode) {
        kclass.safeCast(node)?.apply {
            try {
                assertions()
            } catch (e: AssertionError) {
                throw MatcherError(
                    "${e.message.orEmpty()}\nin node:\n${node.debugRepresentation()}",
                    this@NodeMatcher,
                    cause = e
                )
            }
        } ?: throw MatcherError("Expected ${kclass.simpleName} but got:\n${node.debugRepresentation()}", this)
    }
}

class CompositeMatcher<T : ContentComposite>(
    kclass: KClass<T>,
    private val children: List<MatcherElement>,
    assertions: T.() -> Unit = {}
) : NodeMatcher<T>(kclass, assertions) {
    internal val normalizedChildren: List<MatcherElement> by lazy {
        children.fold(listOf()) { acc, e ->
            when {
                acc.lastOrNull() is Anything && e is Anything -> acc
                acc.lastOrNull() is TextMatcher && e is TextMatcher ->
                    acc.dropLast(1) + TextMatcher((acc.lastOrNull() as TextMatcher).text + e.text)
                else -> acc + e
            }
        }
    }

    override fun tryMatch(node: ContentNode) {
        super.tryMatch(node)
        kclass.cast(node).children.asSequence()
            .filter { it !is ContentText || it.text.isNotBlank() }
            .fold(FurtherSiblings(normalizedChildren, this).pop()) { acc, n -> acc.next(n) }.finish()
    }
}

object Anything : MatcherElement()

private sealed class MatchWalkerState {
    abstract fun next(node: ContentNode): MatchWalkerState
    abstract fun finish()
}

private class TextMatcherState(
    val text: String,
    val rest: FurtherSiblings,
    val anchor: TextMatcher
) : MatchWalkerState() {
    override fun next(node: ContentNode): MatchWalkerState {
        node as? ContentText ?: throw MatcherError("Expected text: \"$text\" but got\n${node.debugRepresentation()}", anchor)
        return when {
            text == node.text -> rest.pop()
            text.startsWith(node.text) -> TextMatcherState(text.removePrefix(node.text), rest, anchor)
            else -> throw MatcherError("Expected text: \"$text\", but got: \"${node.text}\"", anchor)
        }
    }

    override fun finish() = throw MatcherError("\"$text\" was not found" + rest.messageEnd, anchor)
}

private class EmptyMatcherState(val parent: CompositeMatcher<*>) : MatchWalkerState() {
    override fun next(node: ContentNode): MatchWalkerState {
        throw MatcherError("Unexpected node:\n${node.debugRepresentation()}", parent, anchorAfter = true)
    }

    override fun finish() = Unit
}

private class NodeMatcherState(
    val matcher: NodeMatcher<*>,
    val rest: FurtherSiblings
) : MatchWalkerState() {
    override fun next(node: ContentNode): MatchWalkerState {
        matcher.tryMatch(node)
        return rest.pop()
    }

    override fun finish() =
        throw MatcherError("Content of type ${matcher.kclass} was not found" + rest.messageEnd, matcher)
}

private class SkippingMatcherState(
    val innerState: MatchWalkerState
) : MatchWalkerState() {
    override fun next(node: ContentNode): MatchWalkerState = runCatching { innerState.next(node) }.getOrElse { this }

    override fun finish() = innerState.finish()
}

private class FurtherSiblings(val list: List<MatcherElement>, val parent: CompositeMatcher<*>) {
    fun pop(): MatchWalkerState = when (val head = list.firstOrNull()) {
        is TextMatcher -> TextMatcherState(head.text, drop(), head)
        is NodeMatcher<*> -> NodeMatcherState(head, drop())
        is Anything -> SkippingMatcherState(drop().pop())
        null -> EmptyMatcherState(parent)
    }

    fun drop() = FurtherSiblings(list.drop(1), parent)

    val messageEnd: String
        get() = list.count { it !is Anything }.takeIf { it > 0 }
            ?.let { " and $it further matchers were not satisfied" } ?: ""
}


internal fun MatcherElement.toDebugString(anchor: MatcherElement?, anchorAfter: Boolean): String {
    fun Appendable.append(element: MatcherElement, ownPrefix: String, childPrefix: String) {
        if (anchor != null) {
            if (element != anchor || anchorAfter) append(" ".repeat(4))
            else append("--> ")
        }

        append(ownPrefix)
        when (element) {
            is Anything -> append("skipAllNotMatching\n")
            is TextMatcher -> append("\"${element.text}\"\n")
            is CompositeMatcher<*> -> {
                append("${element.kclass.simpleName.toString()}\n")
                if (element.normalizedChildren.isNotEmpty()) {
                    val newOwnPrefix = "$childPrefix├─ "
                    val lastOwnPrefix = "$childPrefix└─ "
                    val newChildPrefix = "$childPrefix│  "
                    val lastChildPrefix = "$childPrefix   "
                    element.normalizedChildren.forEachIndexed { n, e ->
                        if (n != element.normalizedChildren.lastIndex) append(e, newOwnPrefix, newChildPrefix)
                        else append(e, lastOwnPrefix, lastChildPrefix)
                    }
                }
                if (element == anchor && anchorAfter) {
                    append("--> $childPrefix\n")
                }
            }
            is NodeMatcher<*> -> append("${element.kclass.simpleName}\n")
        }
    }

    return buildString { append(this@toDebugString, "", "") }
}

private fun ContentNode.debugRepresentation() = asPrintableTree { element ->
    append(if (element is ContentText) """"${element.text}"""" else element::class.simpleName)
    append(
        " { " +
                "kind=${element.dci.kind}, " +
                "dri=${element.dci.dri}, " +
                "style=${element.style}, " +
                "sourceSets=${element.sourceSets} " +
                "}"
    )
}

data class MatcherError(
    override val message: String,
    val anchor: MatcherElement,
    val anchorAfter: Boolean = false,
    override val cause: Throwable? = null
) : AssertionError(message, cause)

// Creating this whole mechanism was most scala-like experience I had since I stopped using scala.
// I don't know how I should feel about it.
