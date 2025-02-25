package markdown

import org.intellij.markdown.MarkdownElementTypes
import org.jetbrains.dokka.model.doc.*
import org.junit.jupiter.api.Test

class CommonmarkSpecTest : KDocTest() {

    private fun testBlocks(markdown: String, expected: List<DocTag>) {
        val expectedDocumentationNode = DocumentationNode(
            listOf(
                Description(
                    CustomDocTag(
                        expected,
                        name = MarkdownElementTypes.MARKDOWN_FILE.name
                    )
                )
            )
        )
        executeTest(markdown, expectedDocumentationNode)
    }

    private fun testInlines(markdown: String, expected: List<DocTag>) =
        testBlocks(markdown, listOf(P(expected)))


    /**
     * ## Code spans
     *
     * A [backtick string](@)
     * is a string of one or more backtick characters (`` ` ``) that is neither
     * preceded nor followed by a backtick.
     *
     * A [code span](@) begins with a backtick string and ends with
     * a backtick string of equal length.  The contents of the code span are
     * the characters between these two backtick strings, normalized in the
     * following ways:
     *
     * - First, [line endings] are converted to [spaces].
     * - If the resulting string both begins *and* ends with a [space]
     *   character, but does not consist entirely of [space]
     *   characters, a single [space] character is removed from the
     *   front and back.  This allows you to include code that begins
     *   or ends with backtick characters, which must be separated by
     *   whitespace from the opening or closing backtick strings.
     *
     * This is a simple code span:
     */
    @Test fun codeSpans_Example1() = testInlines(
        markdown = "`foo`",
        expected = listOf(CodeInline(listOf(Text("foo"))))
    )

    /**
     * Here two backticks are used, because the code contains a backtick.
     * This example also illustrates stripping of a single leading and
     * trailing space:
     */
    @Test fun codeSpans_Example2() = testInlines(
        markdown = "`` foo ` bar ``",
        expected = listOf(CodeInline(listOf(Text("` foo ` bar `"))))
    /// expected = listOf(CodeInline(listOf(Text("foo ` bar"))))
    )

    /**
     * This example shows the motivation for stripping leading and trailing
     * spaces:
     */
    @Test fun codeSpans_Example3() = testInlines(
        markdown = "` `` `",
        expected = listOf(CodeInline(listOf(Text("`` "))))
    /// expected = listOf(CodeInline(listOf(Text("``"))))
    )

    /**
     * Note that only *one* space is stripped:
     */
    @Test fun codeSpans_Example4() = testInlines(
        markdown = "`  ``  `",
        expected = listOf( CodeInline(listOf(Text("``  "))))
    /// expected = listOf( CodeInline(listOf(Text(" `` "))))
    )

    /**
     * The stripping only happens if the space is on both
     * sides of the string:
     */
    @Test fun codeSpans_Example5() = testInlines(
        markdown = "` a`",
        expected = listOf(CodeInline(listOf(Text("a"))))
    /// expected = listOf(CodeInline(listOf(Text(" a"))))
    )

    /**
     * Only [spaces], and not [unicode whitespace] in general, are
     * stripped in this way:
     */
    @Test fun codeSpans_Example6() = testInlines(
        markdown = "` b `",
        expected = listOf(CodeInline(listOf(Text("b"), Text(" ", params = mapOf("content-type" to "html")))))
    /// expected = listOf(CodeInline(listOf(Text(" b "))))
    )

    /**
     * No stripping occurs if the code span contains only spaces:
     */
    @Test fun codeSpans_Example7() = testInlines(
        markdown = "` `\n`  `",
        expected = listOf(CodeInline(), CodeInline())
    /// expected = listOf(CodeInline(listOf(Text(" ")), Text("\n"), CodeInline(listOf(Text("  ")))
    )

    /**
     * [Line endings] are treated like spaces:
     */
    @Test fun codeSpans_Example8() = testInlines(
        markdown = "``\nfoo\nbar  \nbaz\n``",
        expected = listOf(CodeInline(listOf(Text("` foo bar baz `"))))
    /// expected = listOf(CodeInline(listOf(Text("foo bar   baz"))))
    )

    @Test fun codeSpans_Example9() = testInlines(
        markdown = "``\nfoo \n``",
        expected = listOf(CodeInline(listOf(Text("` foo `"))))
    /// expected = listOf(CodeInline(listOf(Text("foo "))))
    )

    /**
     * Interior spaces are not collapsed:
     */
    @Test fun codeSpans_Example10() = testInlines(
        markdown = "`foo   bar \nbaz`",
        expected = listOf(CodeInline(listOf(Text("foo   bar baz"))))
    /// expected = listOf(CodeInline(listOf(Text("foo   bar  baz"))))
    )

    /**
     * Note that browsers will typically collapse consecutive spaces
     * when rendering `<code>` elements, so it is recommended that
     * the following CSS be used:
     *
     *     code{white-space: pre-wrap;}
     *
     *
     * Note that backslash escapes do not work in code spans. All backslashes
     * are treated literally:
     */
    @Test fun codeSpans_Example11() = testInlines(
        markdown = "`foo\\`bar`",
        expected = listOf(CodeInline(listOf(Text("foo\\"))), Text("bar`"))
    )

    /**
     * Backslash escapes are never needed, because one can always choose a
     * string of *n* backtick characters as delimiters, where the code does
     * not contain any strings of exactly *n* backtick characters.
     */
    @Test fun codeSpans_Example12() = testInlines(
        markdown = "``foo`bar``",
        expected = listOf(CodeInline(listOf(Text("`foo`bar`"))))
    /// expected = listOf(CodeInline(listOf(Text("foo`bar"))))
    )

    @Test fun codeSpans_Example13() = testInlines(
        markdown = "` foo `` bar `",
        expected = listOf(CodeInline(listOf(Text("foo `` bar "))))
    /// expected = listOf(CodeInline(listOf(Text("foo `` bar"))))
    )

    /**
     * Code span backticks have higher precedence than any other inline
     * constructs except HTML tags and autolinks.  Thus, for example, this is
     * not parsed as emphasized text, since the second `*` is part of a code
     * span:
     */
    @Test fun codeSpans_Example14() = testInlines(
        markdown = "*foo`*`",
        expected = listOf(Text("foo"), CodeInline(listOf(Text("*"))))
    /// expected = listOf(Text("*foo"), CodeInline(listOf(Text("*"))))
    )

    /**
     * And this is not parsed as a link:
     */
    @Test fun codeSpans_Example15() = testInlines(
        markdown = "[not a `link](/foo`)",
        expected = listOf(Text("[not a "), CodeInline(listOf(Text("link](/foo"))), Text(")"))
    )

    /**
     * Code spans, HTML tags, and autolinks have the same precedence.
     * Thus, this is code:
     */
    @Test fun codeSpans_Example16() = testInlines(
        markdown = "`<a href=\"`\">`",
        expected = listOf(CodeInline())
    /// expected = listOf(CodeInline(listOf(Text("<a href=\""))), Text("\">`"))
    )

    /**
     * But this is an HTML tag:
     */
    @Test fun codeSpans_Example17() = testInlines(
        markdown = "<a href=\"`\">`",
        expected = listOf(Text("<a href=\"`\">", params = mapOf("content-type" to "html")), Text("`"))
    )

    /**
     * And this is code:
     */
    @Test fun codeSpans_Example18() = testInlines(
        markdown = "`<http://foo.bar.`baz>`",
        expected = listOf(CodeInline())
    /// expected = listOf(CodeInline(listOf(Text("<http://foo.bar."))), Text("baz>`"))
    )

    /**
     * But this is an autolink:
     */
    @Test fun codeSpans_Example19() = testInlines(
        markdown = "<http://foo.bar.`baz>`",
        expected = listOf(A(listOf(Text("http://foo.bar.`baz")), mapOf("href" to "http://foo.bar.`baz")), Text("`"))
    )

    /**
     * When a backtick string is not closed by a matching backtick string,
     * we just have literal backticks:
     */
    @Test fun codeSpans_Example20() = testInlines(
        markdown = "```foo``",
        expected = listOf(Text("```foo``"))
    )

    @Test fun codeSpans_Example21() = testInlines(
        markdown = "`foo",
        expected = listOf(Text("`foo"))
    )

    /**
     * The following case also illustrates the need for opening and
     * closing backtick strings to be equal in length:
     */
    @Test fun codeSpans_Example22() = testInlines(
        markdown = "`foo``bar``",
        expected = listOf(Text("`foo"), CodeInline(listOf(Text("`bar`"))))
    /// expected = listOf(Text("`foo"), CodeInline(listOf(Text("bar"))))
    )

    /**
     * ## Hard line breaks
     *
     * A line ending (not in a code span or HTML tag) that is preceded
     * by two or more spaces and does not occur at the end of a block
     * is parsed as a [hard line break](@) (rendered
     * in HTML as a `<br />` tag):
     */
    @Test fun hardLineBreaks_Example1() = testInlines(
        markdown = "foo  \nbaz",
        expected = listOf(Text("foo baz"))
    /// expected = listOf(Text("foo"), Br, Text("baz"))
    )

    /**
     * For a more visible alternative, a backslash before the
     * [line ending] may be used instead of two or more spaces:
     */
    @Test fun hardLineBreaks_Example2() = testInlines(
        markdown = "foo\\\nbaz",
        expected = listOf(Text("foo"), Br, Text("baz"))
    )

    /**
     * More than two spaces can be used:
     */
    @Test fun hardLineBreaks_Example3() = testInlines(
        markdown = "foo       \nbaz",
        expected = listOf(Text("foo baz"))
    /// expected = listOf(Text("foo"), Br, Text("baz"))
    )

    /**
     * Leading spaces at the beginning of the next line are ignored:
     */
    @Test fun hardLineBreaks_Example4() = testInlines(
        markdown = "foo  \n     bar",
        expected = listOf(Text("foo     bar"))
    /// expected = listOf(Text("foo"), Br, Text("bar"))
    )

    @Test fun hardLineBreaks_Example5() = testInlines(
        markdown = "foo\\\n     bar",
        expected = listOf(Text("foo"), Br, Text("    bar"))
    /// expected = listOf(Text("foo"), Br, Text("bar"))
    )

    /**
     * Hard line breaks can occur inside emphasis, links, and other constructs
     * that allow inline content:
     */
    @Test fun hardLineBreaks_Example6() = testInlines(
        markdown = "*foo  \nbar*",
        expected = listOf(Text("foo bar*"))
    /// expected = listOf(Em(listOf(Text("foo"), Br, Text("bar"))))
    )

    @Test fun hardLineBreaks_Example7() = testInlines(
        markdown = "*foo\\\nbar*",
        expected = listOf(Text("foo"), Br, Text("bar*"))
    /// expected = listOf(Em(listOf(Text("foo"), Br, Text("bar"))))
    )

    /**
     * Hard line breaks do not occur inside code spans
     */
    @Test fun hardLineBreaks_Example8() = testInlines(
        markdown = "`code  \nspan`",
        expected = listOf(CodeInline(listOf(Text("code span"))))
    /// expected = listOf(CodeInline(listOf(Text("code   span"))))
    )

    @Test fun hardLineBreaks_Example9() = testInlines(
        markdown = "`code\\\nspan`",
        expected = listOf(CodeInline(listOf(Text("code\\ span")))),
    )

    /**
     * or HTML tags:
     */
    @Test fun hardLineBreaks_Example10() = testInlines(
        markdown = "<a href=\"foo  \nbar\">",
        expected = listOf()
    /// "<a href=\"foo  \nbar\">"
    )

    @Test fun hardLineBreaks_Example11() = testInlines(
        markdown = "<a href=\"foo\\\nbar\">",
        expected = listOf(Br, Text("bar\""), Text(">", params = mapOf("content-type" to "html"))),
    /// "<a href=\"foo\\\nbar\">"
    )

    /**
     * Hard line breaks are for separating inline content within a block.
     * Neither syntax for hard line breaks works at the end of a paragraph or
     * other block element:
     */
    @Test fun hardLineBreaks_Example12() = testInlines(
        markdown = "foo\\",
        expected = listOf(Text("foo\\"))
    )

    @Test fun hardLineBreaks_Example13() = testInlines(
        markdown = "foo  ",
        expected = listOf(Text("foo"))
    )

    @Test fun hardLineBreaks_Example14() = testBlocks(
        markdown = "### foo\\",
        expected = listOf(H3(listOf(Text(" foo\\"))))
    /// expected = listOf(H3(listOf(Text("foo\\"))))
    )

    @Test fun hardLineBreaks_Example15() = testBlocks(
        markdown = "### foo  ",
        expected = listOf(H3(listOf(Text(" foo"))))
    /// expected = listOf(H3(listOf(Text("foo"))))
    )

    /**
     * ## Soft line breaks
     *
     * A regular line ending (not in a code span or HTML tag) that is not
     * preceded by two or more spaces or a backslash is parsed as a
     * [softbreak](@).  (A soft line break may be rendered in HTML either as a
     * [line ending] or as a space. The result will be the same in
     * browsers. In the examples here, a [line ending] will be used.)
     */
    @Test fun softLineBreaks_Example1() = testInlines(
        markdown = "foo\nbaz",
        expected = listOf(Text("foo baz"))
    /// expected = listOf(Text("foo\nbaz"))
    )

    /**
     * Spaces at the end of the line and beginning of the next line are
     * removed:
     */
    @Test fun softLineBreaks_Example2() = testInlines(
        markdown = "foo \n baz",
        expected = listOf(Text("foo baz"))
    /// expected = listOf(Text("foo\nbaz"))
    )

    /**
     * A conforming parser may render a soft line break in HTML either as a
     * line ending or as a space.
     *
     * A renderer may also provide an option to render soft line breaks
     * as hard line breaks.
     *
     *
     * ## Textual content
     *
     * Any characters not given an interpretation by the above rules will
     * be parsed as plain textual content.
     */
    @Test fun textualContent_Example1() = testInlines(
        markdown = "hello \$.;'there",
        expected = listOf(Text("hello \$.;'there"))
    )

    @Test fun textualContent_Example2() = testInlines(
        markdown = "Foo χρῆν",
        expected = listOf(Text("Foo χρῆν"))
    )

    /**
     * Internal spaces are preserved verbatim:
     */
    @Test fun textualContent_Example3() = testInlines(
        markdown = "Multiple     spaces",
        expected = listOf(Text("Multiple     spaces"))
    )

}
