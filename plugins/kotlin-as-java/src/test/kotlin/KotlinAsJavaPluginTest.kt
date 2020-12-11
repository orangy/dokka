package kotlinAsJavaPlugin

import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.jupiter.api.Test
import matchers.content.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.jdk
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DInterface
import org.junit.Assert
import signatures.firstSignature
import signatures.renderedContent
import signatures.signature
import utils.A
import utils.Span
import utils.TestOutputWriterPlugin
import utils.match
import kotlin.test.assertEquals

class KotlinAsJavaPluginTest : BaseAbstractTest() {

    @Test
    fun topLevelTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |object TestObj {}
            |
            |fun testFL(l: List<String>) = l
            |fun testF() {}
            |fun testF2(i: Int) = i
            |fun testF3(to: TestObj) = to
            |fun <T : Char> testF4(t: T) = listOf(t)
            |val testV = 1
        """.trimMargin(),
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val content = (root.children.single().children.first { it.name == "TestKt" } as ContentPage).content

                val children = content.mainContents.first().cast<ContentGroup>()
                    .children.filterIsInstance<ContentTable>()
                    .filter { it.children.isNotEmpty() }

                children.assertCount(2)
            }
        }
    }

    @Test
    fun topLevelWithClassTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |class Test {
            |   fun testFC() {}
            |   val testVC = 1
            |}
            |
            |fun testF(i: Int) = i
            |val testV = 1
        """.trimMargin(),
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val contentList = root.children
                    .flatMap { it.children<ContentPage>() }
                    .map { it.content }

                val children = contentList.flatMap { content ->
                    content.mainContents.first().cast<ContentGroup>().children
                        .filterIsInstance<ContentTable>()
                        .filter { it.children.isNotEmpty() }
                }

                children.assertCount(4)
            }
        }
    }

    @Test
    fun kotlinAndJavaTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |fun testF(i: Int) = i
            |/src/main/kotlin/kotlinAsJavaPlugin/TestJ.java
            |package kotlinAsJavaPlugin
            |
            |class TestJ {
            |   int testF(int i) { return i; }
            |}
        """.trimMargin(),
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val classes = root.children.first().children.associateBy { it.name }
                classes.values.assertCount(2, "Class count: ")

                classes["TestKt"].let {
                    it?.children.orEmpty().assertCount(1, "(Kotlin) TestKt members: ")
                    it!!.children.first()
                        .let { assert(it.name == "testF") { "(Kotlin) Expected method name: testF, got: ${it.name}" } }
                }

                classes["TestJ"].let {
                    it?.children.orEmpty().assertCount(1, "(Java) TestJ members: ")
                    it!!.children.first()
                        .let { assert(it.name == "testF") { "(Java) Expected method name: testF, got: ${it.name}" } }
                }
            }
        }
    }

    @Test
    fun `public kotlin properties should have a getter with same visibilities`(){
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |class Test {
            |   public val publicProperty: String = ""
            |}
        """.trimMargin(),
            configuration,
            cleanupOutput = true
        ) {
            pagesTransformationStage = { rootPageNode ->
                val propertyGetter = rootPageNode.dfs { it is MemberPageNode && it.name == "getPublicProperty" } as? MemberPageNode
                assert(propertyGetter != null)
                propertyGetter!!.content.assertNode {
                    group {
                        header(1) {
                            +"getPublicProperty"
                        }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                group {
                                    +"final "
                                    group {
                                        link {
                                            +"String"
                                        }
                                    }
                                    link {
                                        +"getPublicProperty"
                                    }
                                    +"()"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `java properties should keep its modifiers`(){
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/TestJ.java
            |package kotlinAsJavaPlugin
            |
            |class TestJ {
            |   public Int publicProperty = 1;
            |}
        """.trimMargin(),
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val testClass = root.dfs { it.name == "TestJ" } as? ClasslikePageNode
                assert(testClass != null)
                (testClass!!.content as ContentGroup).children.last().assertNode {
                    group {
                        header(2){
                            +"Properties"
                        }
                        table {
                            group {
                                link {
                                    +"publicProperty"
                                }
                                platformHinted {
                                    group {
                                        +"public Int"
                                        link {
                                            +"publicProperty"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `koltin interfaces and classes should be split to extends and implements`(){
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |open class A { }
            |interface B
            |class C : A(), B
        """.trimMargin(),
            configuration,
            cleanupOutput = true
        ) {
            pagesGenerationStage = { root ->
                val testClass = root.dfs { it.name == "C" } as? ClasslikePageNode
                assert(testClass != null)
                testClass!!.content.assertNode {
                    group {
                        header(expectedLevel = 1) {
                            +"C"
                        }
                        platformHinted {
                            group {
                                +"public final class "
                                link {
                                    +"C"
                                }
                                +" extends "
                                group {
                                    link {
                                        +"A"
                                    }
                                }
                                +" implements "
                                group {
                                    link {
                                        +"B"
                                    }
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    private fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assert(count() == n) { "${prefix}Expected $n, got ${count()}" }

    @Test
    fun `typealias`() {
        val writerPlugin = TestOutputWriterPlugin()
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    externalDocumentationLinks = listOf(DokkaConfiguration.ExternalDocumentationLink.jdk(8))
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |typealias XD = Int
            |class ABC {
            |    fun someFun(xd: XD): Int = 1
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-a-b-c/some-fun.html").signature().first().match(
                    "final ", A("Integer"), A("someFun"), "(", A("Integer"), A("xd"), ")", Span()
                )
            }
        }
    }

    @Test
    fun `typealias with generic`() {
        val writerPlugin = TestOutputWriterPlugin()
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    externalDocumentationLinks = listOf(
                        DokkaConfiguration.ExternalDocumentationLink.jdk(8),
                        stdlibExternalDocumentationLink
                    )
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |typealias XD<B, A> = Map<A, B>
            |
            |class ABC {
            |    fun someFun(xd: XD<Int, String>) = 1
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            renderingStage = { _, _ ->
                writerPlugin.writer.renderedContent("root/kotlinAsJavaPlugin/-a-b-c/some-fun.html").signature().first().match(
                    "final ", A("Integer"), A("someFun"), "(", A("Map"), "<", A("String"),
                    ", ", A("Integer"), ">", A("xd"), ")", Span()
                )
            }
        }
    }

    @Test
    fun `const in top level`() {
        val writerPlugin = TestOutputWriterPlugin()
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    externalDocumentationLinks = listOf(
                        DokkaConfiguration.ExternalDocumentationLink.jdk(8),
                        stdlibExternalDocumentationLink
                    )
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |const val FIRST = "String"
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            renderingStage = { _, _ ->
                Assert.assertNull(writerPlugin.writer.contents["root/kotlinAsJavaPlugin/-test-kt/get-f-i-r-s-t.html"])
            }
        }
    }

    @Test
    fun `extract const val from companion object`() {
        val writerPlugin = TestOutputWriterPlugin()
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    externalDocumentationLinks = listOf(
                        DokkaConfiguration.ExternalDocumentationLink.jdk(8),
                        stdlibExternalDocumentationLink
                    )
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |class TestClass {
            |   companion object {
            |       const val someVal = 1
            |   }
            |}
            |
            |interface TestInterface {
            |   companion object {
            |       const val someVal = 1
            |   }
            |}
            |
            |enum class TestEnum {
            |   companion object {
            |       const val someVal = 1
            |   }
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            documentablesTransformationStage = { module ->
                val pkg = module.packages.first()
                val clazz = pkg.children.first { it is DClass } as DClass
                assertEquals("someVal", clazz.properties.first().name)
                assertEquals(1, clazz.classlikes.first().properties.size)
                val intf = pkg.children.first { it is DInterface } as DInterface
                assertEquals("someVal", intf.properties.first().name)
                assertEquals(1, intf.classlikes.first().properties.size)
                val enum = pkg.children.first { it is DEnum } as DEnum
                assertEquals("someVal", enum.properties.first().name)
                assertEquals(1, enum.classlikes.first().properties.size)
            }
        }
    }

    @Test
    fun testJvmName() {
        val writerPlugin = TestOutputWriterPlugin()
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                    externalDocumentationLinks = listOf(
                        DokkaConfiguration.ExternalDocumentationLink.jdk(8),
                        stdlibExternalDocumentationLink
                    )
                    analysisPlatform = "jvm"
                    classpath += listOfNotNull(jvmStdlibPath)
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/TestJvmName.kt
            |@file:JvmName("TestJava")
            |
            |package kotlinAsJavaPlugin
            |
            |@JvmName("someFunInJava")
            |fun someFun(a: Int) = 1
            |
            |@get:JvmName("findSomeVar")
            |@set:JvmName("modifySomeVar")
            |var someVar: Int = 1
            |
            |val otherVal: Int
            |   @JvmName("findOtherVal") get() = 1
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin),
            cleanupOutput = true
        ) {
            renderingStage = { _, _ ->
                writerPlugin
                    .writer
                    .renderedContent("root/kotlinAsJavaPlugin/-test-java/index.html")
                    .firstSignature()
                    .match(
                        "public final class ", A("TestJava"), Span()
                    )
                writerPlugin
                    .writer
                    .renderedContent("root/kotlinAsJavaPlugin/-test-java/some-fun-in-java.html")
                    .firstSignature()
                    .match(
                        "final static ", A("Integer"), A("someFunInJava"), "(", A("Integer"), A("a"), ")", Span()
                    )
                writerPlugin
                    .writer
                    .renderedContent("root/kotlinAsJavaPlugin/-test-java/find-some-var.html")
                    .firstSignature()
                    .match(
                        "static ", A("Integer"), A("findSomeVar"), "()", Span()
                    )
                writerPlugin
                    .writer
                    .renderedContent("root/kotlinAsJavaPlugin/-test-java/modify-some-var.html")
                    .firstSignature()
                    .match(
                        "static ", A("Unit"), A("modifySomeVar"), "(", A("Integer"), A("someVar"), ")", Span()
                    )
            }
        }
    }
}

private val ContentNode.mainContents: List<ContentNode>
    get() = (this as ContentGroup).children
    .filterIsInstance<ContentGroup>()
    .single { it.dci.kind == ContentKind.Main }.children
