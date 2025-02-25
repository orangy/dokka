/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package locationProvider

import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.TypeConstructor
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.plugability.DokkaContext
import utils.TestOutputWriterPlugin
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultExternalLocationProviderTest : BaseAbstractTest() {
    private val testDataDir =
        getTestDataDir("locationProvider").toAbsolutePath().toString().removePrefix("/").let { "/$it" }
    private val kotlinLang = "https://kotlinlang.org/api/core"
    private val packageListURL = URL("file://$testDataDir/stdlib-package-list")
    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    private fun getTestLocationProvider(context: DokkaContext? = null): DefaultExternalLocationProvider {
        val dokkaContext = context ?: DokkaContext.create(configuration, logger, emptyList())
        val packageList = PackageList.load(packageListURL, 8, true)!!
        val externalDocumentation =
            ExternalDocumentation(URL(kotlinLang), packageList)
        return DefaultExternalLocationProvider(externalDocumentation, ".html", dokkaContext)
    }

    @Test
    fun `ordinary link`() {
        val locationProvider = getTestLocationProvider()
        val dri = DRI("kotlin.reflect", "KVisibility")

        assertEquals("$kotlinLang/kotlin.reflect/-k-visibility/index.html", locationProvider.resolve(dri))
    }

    @Test
    fun `relocation in package list`() {
        val locationProvider = getTestLocationProvider()
        val dri = DRI(
            "",
            "",
            Callable(
                "longArray",
                null,
                listOf(
                    TypeConstructor("kotlin.Int", emptyList()),
                    TypeConstructor("kotlin.Any", emptyList())
                )
            )
        )

        assertEquals("$kotlinLang/kotlin-stdlib/[JS root]/long-array.html", locationProvider.resolve(dri))
    }

    @Test
    fun `should return null for class not in list`() {
        val locationProvider = getTestLocationProvider()
        val dri = DRI(
            "foo",
            "Bar"
        )

        assertEquals(null, locationProvider.resolve(dri))
    }

    @Test
    fun `should have a correct url to an external inherited member #2879`() {
        val writerPlugin = TestOutputWriterPlugin()
        val configuration = dokkaConfiguration {


            sourceSets {
                sourceSet {
                    externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
                    sourceRoots = listOf("src/")
                    classpath = listOfNotNull(jvmStdlibPath)
                }
            }
        }

        testInline(
            """
            /src/kotlin/main.kt
            open interface C : Collection<C>
            interface A : C()
            interface B : C()
            """.trimIndent()
            ,
            pluginOverrides = listOf(writerPlugin),
            configuration = configuration
        ) {
            renderingStage = { rootPage, ctx ->
                val location = DokkaLocationProvider(rootPage, ctx, ".html")
                val classA = rootPage.dfs { it is ClasslikePageNode && it.name == "A" }
                val classB = rootPage.dfs { it is ClasslikePageNode && it.name == "B" }
                val classC = rootPage.dfs { it is ClasslikePageNode && it.name == "C" }
                val sourceSet = (classA as ClasslikePageNode).content.sourceSets
                val dri = DRI("kotlin.collections", "Collection", Callable(name="isEmpty", receiver=null, params=emptyList()))
                assertEquals("https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/is-empty.html", location.resolve(dri, sourceSet, classA))
                assertEquals("https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/is-empty.html", location.resolve(dri, sourceSet, classB))
                assertEquals("https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/is-empty.html", location.resolve(dri, sourceSet, classC))
            }
        }
    }

    @Test
    fun `should have external links for external inherited members`() {
        val writerPlugin = TestOutputWriterPlugin()
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    externalDocumentationLinks = listOf(stdlibExternalDocumentationLink)
                    sourceRoots = listOf("src/")
                    classpath = listOfNotNull(jvmStdlibPath)
                }
            }
        }

        testInline(
            """
            /src/kotlin/main.kt
             interface MyCharSequence: CharSequence
            """.trimIndent()
            ,
            pluginOverrides = listOf(writerPlugin),
            configuration = configuration
        ) {
            renderingStage = { _, _ ->
                "".chars()
                val content = writerPlugin.writer.contents["root/[root]/-my-char-sequence/index.html"] ?: ""
                assertTrue(content.contains("<a href=\"https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/length.html\"><span><span>length</span></span></a>"))
                assertTrue(content.contains("<a href=\"https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/get.html\"><span><span>get</span></span></a>"))
                assertTrue(content.contains("<a href=\"https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/sub-sequence.html\"><span>sub</span><wbr></wbr><span><span>Sequence</span></span></a>"))
                // TODO #3542
                // these links are invalid
                // chars() and codePoints() are absent in https://kotlinlang.org/ since they come from mapping Kotlin to Java
                // see https://kotlinlang.org/docs/java-interop.html#mapped-types
                assertTrue(content.contains("<a href=\"https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/chars.html\"><span><span>chars</span></span></a>"))
                assertTrue(content.contains("<a href=\"https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-sequence/code-points.html\"><span>code</span><wbr></wbr><span><span>Points</span></span></a>"))
            }
        }
    }
}
