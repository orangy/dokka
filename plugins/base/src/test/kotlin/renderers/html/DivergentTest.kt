package renderers.html

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.SourceRootImpl
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.pages.ContentDivergentGroup
import org.jetbrains.dokka.pages.TextStyle
import org.junit.jupiter.api.Test
import renderers.Div
import renderers.RenderingOnlyTestBase
import renderers.TestPage
import renderers.match

class DivergentTest : RenderingOnlyTestBase() {
    private val js = SourceSetData("root", "JS", Platform.js, listOf(SourceRootImpl("pl1")))
    private val jvm = SourceSetData("root", "JVM", Platform.jvm, listOf(SourceRootImpl("pl1")))
    private val native = SourceSetData("root", "NATIVE", Platform.native, listOf(SourceRootImpl("pl1")))

    @Test
    fun simpleWrappingCase() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
            }
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div(Div("a")))))
    }

    @Test
    fun noPlatformHintCase() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test"), implicitlySourceSetHinted = false) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
            }
        }
        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div("a")))
    }

    @Test
    fun divergentBetweenSourceSets() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(jvm)) {
                    divergent {
                        text("b")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    divergent {
                        text("c")
                    }
                }
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div(Div("a"), Div("b"), Div("c")))))
    }

    @Test
    fun divergentInOneSourceSet() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("a")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(js)) {
                    divergent {
                        text("b")
                    }
                }
                instance(setOf(DRI("test", "Test3")), setOf(js)) {
                    divergent {
                        text("c")
                    }
                }
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div((Div(Div("abc"))))))
    }

    @Test
    fun divergentInAndBetweenSourceSets() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    divergent {
                        text("a")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("b")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(jvm)) {
                    divergent {
                        text("c")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(js)) {
                    divergent {
                        text("d")
                    }
                }
                instance(setOf(DRI("test", "Test3")), setOf(native)) {
                    divergent {
                        text("e")
                    }
                }
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(Div(Div(Div(Div("ae"), Div("bd"), Div("c")))))
    }

    @Test
    fun divergentInAndBetweenSourceSetsWithGrouping() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    divergent {
                        text("a")
                    }
                    after {
                        text("a+")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(js)) {
                    divergent {
                        text("b")
                    }
                    after {
                        text("bd+")
                    }
                }
                instance(setOf(DRI("test", "Test")), setOf(jvm)) {
                    divergent {
                        text("c")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(js)) {
                    divergent {
                        text("d")
                    }
                    after {
                        text("bd+")
                    }
                }
                instance(setOf(DRI("test", "Test3")), setOf(native)) {
                    divergent {
                        text("e")
                    }
                    after {
                        text("e+")
                    }
                }
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(
            Div(Div(Div(Div("a")), Div(Div())), "a+",),
            Div(Div(Div(Div("bd")), Div(Div())), "bd+"),
            Div(Div(Div(Div("c")), Div(Div()))),
            Div(Div(Div(Div("e")), Div(Div())), "e+")
        )
    }

    @Test
    fun divergentSameBefore() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    before {
                        text("ab-")
                    }
                    divergent {
                        text("a")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(native)) {
                    before {
                        text("ab-")
                    }
                    divergent {
                        text("b")
                    }
                }
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(
            Div(
                "ab-",
                Div(Div(Div("ab")))
            )
        )
    }

    @Test
    fun divergentSameAfter() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    divergent {
                        text("a")
                    }
                    after {
                        text("ab+")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(native)) {
                    divergent {
                        text("b")
                    }
                    after {
                        text("ab+")
                    }
                }
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(
            Div(
                Div(Div(Div("ab"))),
                "ab+"
            )
        )
    }

    @Test
    fun divergentGroupedByBeforeAndAfter() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    before {
                        text("ab-")
                    }
                    divergent {
                        text("a")
                    }
                    after {
                        text("ab+")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(native)) {
                    before {
                        text("ab-")
                    }
                    divergent {
                        text("b")
                    }
                    after {
                        text("ab+")
                    }
                }
            }
        }

        HtmlRenderer(context).render(page)
        val r = renderedContent
        renderedContent.match(
            Div(
                "ab-",
                Div(Div(Div("ab"))),
                "ab+"
            )
        )
    }

    @Test
    fun divergentDifferentBeforeAndAfter() {
        val page = TestPage {
            divergentGroup(ContentDivergentGroup.GroupID("test")) {
                instance(setOf(DRI("test", "Test")), setOf(native)) {
                    before {
                        text("a-")
                    }
                    divergent {
                        text("a")
                    }
                    after {
                        text("ab+")
                    }
                }
                instance(setOf(DRI("test", "Test2")), setOf(native)) {
                    before {
                        text("b-")
                    }
                    divergent {
                        text("b")
                    }
                    after {
                        text("ab+")
                    }
                }
            }
        }

        HtmlRenderer(context).render(page)
        renderedContent.match(
            Div("a-", Div(Div(Div("a")), Div(Div("NATIVE"))), "ab+"),
            Div("b-", Div(Div(Div("b")), Div(Div("NATIVE"))), "ab+")
        )
    }
}