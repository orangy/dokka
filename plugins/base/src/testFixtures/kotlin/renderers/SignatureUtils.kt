package signatures

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import utils.Tag
import utils.TestOutputWriter

fun TestOutputWriter.renderedContent(path: String = "root/example.html"): Element =
    contents.getValue(path).let { Jsoup.parse(it) }.select("#content")
        .single()

fun Element.signature(): Elements = select("div.symbol.monospace")
fun Element.tab(tabName: String): Elements = select("div[data-togglable=\"$tabName\"]")
fun Element.firstSignature(): Element = signature().first() ?: throw NoSuchElementException("No signature found")
fun Element.lastSignature(): Element = signature().last() ?: throw NoSuchElementException("No signature found")

class Parameters(vararg matchers: Any) : Tag("span", *matchers, expectedClasses = listOf("parameters"))
class Parameter(vararg matchers: Any) : Tag("span", *matchers, expectedClasses = listOf("parameter"))