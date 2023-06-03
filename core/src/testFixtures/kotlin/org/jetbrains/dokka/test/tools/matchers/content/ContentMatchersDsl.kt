package matchers.content

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.test.tools.matchers.content.*
import kotlin.reflect.KClass

// entry point:
fun ContentNode.assertNode(block: ContentMatcherBuilder<ContentComposite>.() -> Unit) {
    val matcher = ContentMatcherBuilder(ContentComposite::class).apply(block).build()
    try {
        matcher.tryMatch(this)
    } catch (e: MatcherError) {
        throw AssertionError(e.message + "\n" + matcher.toDebugString(e.anchor, e.anchorAfter))
    }
}


// DSL:
@DslMarker
annotation class ContentMatchersDsl

@ContentMatchersDsl
class ContentMatcherBuilder<T : ContentComposite> @PublishedApi internal constructor(private val kclass: KClass<T>) {
    @PublishedApi
    internal val children = mutableListOf<MatcherElement>()
    internal val assertions = mutableListOf<T.() -> Unit>()

    fun build() = CompositeMatcher(kclass, childrenOrSkip()) { assertions.forEach { it() } }

    // part of DSL that cannot be defined as an extension
    operator fun String.unaryPlus() {
        children += TextMatcher(this)
    }

    private fun childrenOrSkip() = if (children.isEmpty() && assertions.isNotEmpty()) listOf(Anything) else children
}

fun <T : ContentComposite> ContentMatcherBuilder<T>.check(assertion: T.() -> Unit) {
    assertions += assertion
}

private val ContentComposite.extractedText
    get() = withDescendants().filterIsInstance<ContentText>().joinToString(separator = "") { it.text }

fun <T : ContentComposite> ContentMatcherBuilder<T>.hasExactText(expected: String) {
    assertions += {
        assertThat(this::extractedText).isEqualTo(expected)
    }
}

inline fun <reified S : ContentComposite> ContentMatcherBuilder<*>.composite(
    block: ContentMatcherBuilder<S>.() -> Unit
) {
    children += ContentMatcherBuilder(S::class).apply(block).build()
}

inline fun <reified S : ContentNode> ContentMatcherBuilder<*>.node(noinline assertions: S.() -> Unit = {}) {
    children += NodeMatcher(S::class, assertions)
}

fun ContentMatcherBuilder<*>.skipAllNotMatching() {
    children += Anything
}


// Convenience functions:
fun ContentMatcherBuilder<*>.group(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) = composite(block)

fun ContentMatcherBuilder<*>.tabbedGroup(
    block: ContentMatcherBuilder<ContentGroup>.() -> Unit
) = composite<ContentGroup> {
    block()
    check { assertThat(this::style).transform { style -> style.contains(ContentStyle.TabbedContent) }.isEqualTo(true) }
}

fun ContentMatcherBuilder<*>.tab(
    tabbedContentType: TabbedContentType, block: ContentMatcherBuilder<ContentGroup>.() -> Unit
) = composite<ContentGroup> {
    block()
    check {
        assertThat(this::extra).transform { extra -> extra[TabbedContentTypeExtra]?.value }
            .isEqualTo(tabbedContentType)
    }
}

fun ContentMatcherBuilder<*>.header(expectedLevel: Int? = null, block: ContentMatcherBuilder<ContentHeader>.() -> Unit) =
    composite<ContentHeader> {
        block()
        check { if (expectedLevel != null) assertThat(this::level).isEqualTo(expectedLevel) }
    }

fun ContentMatcherBuilder<*>.p(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) =
    composite<ContentGroup> {
        block()
        check { assertThat(this::style).contains(TextStyle.Paragraph) }
    }

fun ContentMatcherBuilder<*>.link(block: ContentMatcherBuilder<ContentLink>.() -> Unit) = composite(block)

fun ContentMatcherBuilder<*>.table(block: ContentMatcherBuilder<ContentTable>.() -> Unit) = composite(block)

fun ContentMatcherBuilder<*>.platformHinted(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) =
    composite<PlatformHintedContent> { group(block) }

fun ContentMatcherBuilder<*>.list(block: ContentMatcherBuilder<ContentList>.() -> Unit) = composite(block)

fun ContentMatcherBuilder<*>.codeBlock(block: ContentMatcherBuilder<ContentCodeBlock>.() -> Unit) = composite(block)

fun ContentMatcherBuilder<*>.codeInline(block: ContentMatcherBuilder<ContentCodeInline>.() -> Unit) = composite(block)

fun ContentMatcherBuilder<*>.caption(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) = composite<ContentGroup> {
    block()
    check { assertThat(this::style).contains(ContentStyle.Caption) }
}

fun ContentMatcherBuilder<*>.br() = node<ContentBreakLine>()

fun ContentMatcherBuilder<*>.somewhere(block: ContentMatcherBuilder<*>.() -> Unit) {
    skipAllNotMatching()
    block()
    skipAllNotMatching()
}

fun ContentMatcherBuilder<*>.divergentGroup(block: ContentMatcherBuilder<ContentDivergentGroup>.() -> Unit) =
    composite(block)

fun ContentMatcherBuilder<ContentDivergentGroup>.divergentInstance(block: ContentMatcherBuilder<ContentDivergentInstance>.() -> Unit) =
    composite(block)

fun ContentMatcherBuilder<ContentDivergentInstance>.before(block: ContentMatcherBuilder<ContentComposite>.() -> Unit) =
    composite(block)

fun ContentMatcherBuilder<ContentDivergentInstance>.divergent(block: ContentMatcherBuilder<ContentComposite>.() -> Unit) =
    composite(block)

fun ContentMatcherBuilder<ContentDivergentInstance>.after(block: ContentMatcherBuilder<ContentComposite>.() -> Unit) =
    composite(block)
