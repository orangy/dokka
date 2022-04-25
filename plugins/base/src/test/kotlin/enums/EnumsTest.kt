package enums

import matchers.content.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import signatures.renderedContent
import utils.TestOutputWriterPlugin

class EnumsTest : BaseAbstractTest() {

    @Test
    fun `should preserve enum source ordering for documentables`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   E1,
            |   E2,
            |   E3,
            |   E4,
            |   E5,
            |   E6,
            |   E7,
            |   E8,
            |   E9,
            |   E10
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = { module ->
                val testPackage = module.packages[0]
                assertEquals("testpackage", testPackage.name)

                val testEnum = testPackage.classlikes[0] as DEnum
                assertEquals("TestEnum", testEnum.name)

                val enumEntries = testEnum.entries
                assertEquals(10, enumEntries.count())

                assertEquals("E1", enumEntries[0].name)
                assertEquals("E2", enumEntries[1].name)
                assertEquals("E3", enumEntries[2].name)
                assertEquals("E4", enumEntries[3].name)
                assertEquals("E5", enumEntries[4].name)
                assertEquals("E6", enumEntries[5].name)
                assertEquals("E7", enumEntries[6].name)
                assertEquals("E8", enumEntries[7].name)
                assertEquals("E9", enumEntries[8].name)
                assertEquals("E10", enumEntries[9].name)
            }
        }
    }

    @Test
    fun `should preserve enum source ordering for generated pages`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   E1,
            |   E2,
            |   E3,
            |   E4,
            |   E5,
            |   E6,
            |   E7,
            |   E8,
            |   E9,
            |   E10
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            pagesGenerationStage = { rootPage ->
                val packagePage = rootPage.children[0]
                assertEquals("testpackage", packagePage.name)

                val testEnumNode = packagePage.children[0]
                assertEquals("TestEnum", testEnumNode.name)

                val enumEntries = testEnumNode.children
                assertEquals(10, enumEntries.size)

                assertEquals("E1", enumEntries[0].name)
                assertEquals("E2", enumEntries[1].name)
                assertEquals("E3", enumEntries[2].name)
                assertEquals("E4", enumEntries[3].name)
                assertEquals("E5", enumEntries[4].name)
                assertEquals("E6", enumEntries[5].name)
                assertEquals("E7", enumEntries[6].name)
                assertEquals("E8", enumEntries[7].name)
                assertEquals("E9", enumEntries[8].name)
                assertEquals("E10", enumEntries[9].name)
            }
        }
    }

    @Test
    fun `should preserve enum source ordering for rendered entries`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        val writerPlugin = TestOutputWriterPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   E1,
            |   E2,
            |   E3,
            |   E4,
            |   E5,
            |   E6,
            |   E7,
            |   E8,
            |   E9,
            |   E10
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(writerPlugin)
        ) {
            renderingStage = { rootPage, context ->
                val enumEntriesOnPage = writerPlugin.writer.renderedContent("root/testpackage/-test-enum/index.html")
                    .select("div.table[data-togglable=Entries]")
                    .select("div.table-row")
                    .select("div.keyValue")
                    .select("div.title")
                    .select("a")

                val enumEntries = enumEntriesOnPage.map { it.text() }
                assertEquals(10, enumEntries.size)

                assertEquals("E1", enumEntries[0])
                assertEquals("E2", enumEntries[1])
                assertEquals("E3", enumEntries[2])
                assertEquals("E4", enumEntries[3])
                assertEquals("E5", enumEntries[4])
                assertEquals("E6", enumEntries[5])
                assertEquals("E7", enumEntries[6])
                assertEquals("E8", enumEntries[7])
                assertEquals("E9", enumEntries[8])
                assertEquals("E10", enumEntries[9])
            }
        }
    }

    @Test
    fun `should handle companion object within enum`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package testpackage
            |
            |enum class TestEnum {
            |   E1,
            |   E2;
            |   companion object {}
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { m ->
                m.packages.let { p ->
                    assertTrue(p.isNotEmpty(), "Package list cannot be empty")
                    p.first().classlikes.let { c ->
                        assertTrue(c.isNotEmpty(), "Classlikes list cannot be empty")

                        val enum = c.first() as DEnum
                        assertNotNull(enum.companion)
                    }
                }
            }
        }
    }

    @Test
    fun enumWithConstructor() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/TestEnum.kt
            |package testpackage
            |
            |
            |enum class TestEnum(name: String, index: Int, excluded: Boolean) {
            |   E1("e1", 1, true),
            |   E2("e2", 2, false);
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { m ->
                m.packages.let { p ->
                    p.first().classlikes.let { c ->
                        val enum = c.first() as DEnum
                        val (first, second) = enum.entries.sortedBy { it.name }

                        assertEquals(1, first.extra.allOfType<ConstructorValues>().size)
                        assertEquals(1, second.extra.allOfType<ConstructorValues>().size)
                        assertEquals(listOf(StringConstant("e1"), IntegerConstant(1), BooleanConstant(true)), first.extra.allOfType<ConstructorValues>().first().values.values.first())
                        assertEquals(listOf(StringConstant("e2"), IntegerConstant(2), BooleanConstant(false)), second.extra.allOfType<ConstructorValues>().first().values.values.first())
                    }
                }
            }
            pagesGenerationStage = { module ->
                val entryPage = module.dfs { it.name == "E1" } as ClasslikePageNode
                val signaturePart = (entryPage.content.dfs {
                    it is ContentGroup && it.dci.toString() == "[testpackage/TestEnum.E1///PointingToDeclaration/{\"org.jetbrains.dokka.links.EnumEntryDRIExtra\":{\"key\":\"org.jetbrains.dokka.links.EnumEntryDRIExtra\"}}][Symbol]"
                } as ContentGroup)
                assertEquals("(\"e1\", 1, true)", signaturePart.constructorSignature())
            }
        }
    }

    @Test
    fun enumWithMethods() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/TestEnum.kt
            |package testpackage
            |
            |
            |interface Sample {
            |    fun toBeImplemented(): String
            |}
            |
            |enum class TestEnum: Sample {
            |    E1 {
            |        override fun toBeImplemented(): String = "e1"
            |    }
            |}
        """.trimMargin(),
            configuration
        ) {
            documentablesTransformationStage = { m ->
                m.packages.let { p ->
                    p.first().classlikes.let { c ->
                        val enum = c.first { it is DEnum } as DEnum
                        val first = enum.entries.first()

                        assertEquals(1, first.extra.allOfType<ConstructorValues>().size)
                        assertEquals(emptyList<String>(), first.extra.allOfType<ConstructorValues>().first().values.values.first())
                        assertNotNull(first.functions.find { it.name == "toBeImplemented" })
                    }
                }
            }
        }
    }

    @Test
    fun enumWithAnnotationsOnEntries() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/TestEnum.kt
            |package testpackage
            |
            |enum class TestEnum {
            |    /**
            |       Sample docs for E1
            |    **/
            |    @SinceKotlin("1.3") // This annotation is transparent due to lack of @MustBeDocumented annotation
            |    E1
            |}
        """.trimMargin(),
            configuration
        ) {
            pagesTransformationStage = { m ->
                val entryNode = m.children.first { it.name == "testpackage" }.children.first { it.name == "TestEnum" }.children.firstIsInstance<ClasslikePageNode>()
                val signature = (entryNode.content as ContentGroup).dfs { it is ContentGroup && it.dci.toString() == "[testpackage/TestEnum.E1///PointingToDeclaration/{\"org.jetbrains.dokka.links.EnumEntryDRIExtra\":{\"key\":\"org.jetbrains.dokka.links.EnumEntryDRIExtra\"}}][Cover]" } as ContentGroup

                signature.assertNode {
                    header(1) { +"E1" }
                    platformHinted {
                        group {
                            group {
                                link { +"E1" }
                                +"()"
                            }
                        }
                        group {
                            group {
                                group {
                                    +"Sample docs for E1"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ContentGroup.constructorSignature(): String =
        (children.single() as ContentGroup).children.drop(1).joinToString(separator = "") { (it as ContentText).text }
}
