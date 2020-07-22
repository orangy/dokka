package mathjaxTest

import org.jetbrains.dokka.javadoc.JavadocPlugin
import org.jetbrains.dokka.mathjax.LIB_PATH
import org.jetbrains.dokka.mathjax.MathjaxPlugin
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import utils.TestOutputWriterPlugin

class MathjaxPluginTest : AbstractCoreTest() {
    @Test
    fun noMathjaxTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }
        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            | /**
            | * Just a regular kdoc
            | */
            | fun test(): String = ""
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin, MathjaxPlugin())
        ) {
            renderingStage = {
                    _, _ -> Jsoup
                .parse(writerPlugin.writer.contents["root/example/test.html"])
                .head()
                .select("link, script")
                .let {
                    assert(!it.`is`("[href=$LIB_PATH], [src=$LIB_PATH]"))
                }
            }
        }
    }

    @Test
    fun usingMathjaxTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/test/Test.kt")
                }
            }
        }
        val source =
            """
            |/src/main/kotlin/test/Test.kt
            |package example
            | /**
            |  * Some text here
            |  * \(\alpha_{out} = \alpha_{dst}\)
            |  * \(C_{out} = C_{dst}\)
            |  * @usesMathJax
            |  */
            | fun test(): String = ""
            """.trimIndent()
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            source,
            configuration,
            pluginOverrides = listOf(writerPlugin, MathjaxPlugin(), JavadocPlugin())
        ) {
            renderingStage = {
                    _, _ -> Jsoup
                .parse(writerPlugin.writer.contents["example/TestKt.html"])
                .head()
                .select("link, script")
                .let {
                    assert(it.`is`("[href=$LIB_PATH], [src=$LIB_PATH]"))
                }
            }
        }
    }
}
