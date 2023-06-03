package utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.test.assertEquals

/**
 * Parses it using JSOUP, trims whitespace at the end of the line and asserts if they are equal
 * parsing is required to unify the formatting
 */
fun assertHtmlEqualsIgnoringWhitespace(expected: String, actual: String) {
    val ignoreFormattingSettings = Document.OutputSettings().indentAmount(0).outline(true)
    assertEquals(
        Jsoup.parse(expected).outputSettings(ignoreFormattingSettings).outerHtml().trimSpacesAtTheEndOfLine(),
        Jsoup.parse(actual).outputSettings(ignoreFormattingSettings).outerHtml().trimSpacesAtTheEndOfLine()
    )
}

private fun String.trimSpacesAtTheEndOfLine(): String =
    replace(" \n", "\n")