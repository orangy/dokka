package translators

import com.intellij.openapi.application.PathManager
import kotlinx.coroutines.Job
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.base.translators.descriptors.ExternalDocumentablesProvider
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DInterface
import org.jetbrains.dokka.model.Language
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.cast
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExternalDocumentablesTest : BaseAbstractTest() {
    @Test
    fun `external documentable from java stdlib`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                    classpath += listOf(jvmStdlibPath!!,
                            PathManager.getResourceRoot(Element::class.java, "/org/jsoup/nodes/Element.class")!!
                                .replaceAfter(".jar", ""))

                }
            }
        }

        testInline(
            """
            /src/com/sample/MyList.kt
            import org.jsoup.nodes.Element
            package com.sample
            class MyList: ArrayList<Int>()
            
            class Foo: org.jsoup.nodes.Element("foo")
            """.trimIndent(),
            configuration
        ) {
            lateinit var provider: ExternalDocumentablesProvider
            pluginsSetupStage = {
                provider = it.plugin<DokkaBase>().querySingle { externalDocumentablesProvider }
            }
            val jfoo = org.jsoup.nodes.Element("jfoo")
            println(jfoo::class.java)
            documentablesTransformationStage = { mod ->
                val listEntry = mod.packages.single().classlikes.single { it.name == "MyList" }
                    .cast<DClass>().supertypes.entries.single()
                val listRes = provider.findClasslike(
                    listEntry.value.single().typeConstructor.dri,
                    listEntry.key)!!
                assertEquals("ArrayList", listRes.name)
                assertEquals("java.util/ArrayList///PointingToDeclaration/", listRes.dri.toString())

                val listSupertypes = listRes.cast<DClass>().supertypes.values.single()
                    .map { it.typeConstructor.dri.classNames }
                assertEquals(
                    listOf("AbstractList", "RandomAccess", "Cloneable", "Serializable", "MutableList"),
                    listSupertypes
                )
                assertEquals(Language.JAVA, listRes.sources.values.single().language)
                assertEquals("java.util", listRes.sources.values.single().path)

                val jsoupEntry = mod.packages.single().classlikes.single { it.name == "Foo" }
                    .cast<DClass>().supertypes.entries.single()
                val jsoupRes = provider.findClasslike(
                    jsoupEntry.value.single().typeConstructor.dri,
                    jsoupEntry.key)!!
                assertEquals("Element", jsoupRes.name)
                assertEquals("org.jsoup.nodes/Element///PointingToDeclaration/", jsoupRes.dri.toString())

                val jsoupSupertypes = jsoupRes.cast<DClass>().supertypes.values.single()
                    .map { it.typeConstructor.dri.classNames }
                assertEquals(
                    listOf("Node"),
                    jsoupSupertypes
                )
                assertEquals(Language.JAVA, jsoupRes.sources.values.single().language)
                assertEquals("org.jsoup.nodes", jsoupRes.sources.values.single().path)
            }
        }
    }

    @Test
    fun `external documentable from dependency`() {
        val coroutinesPath =
            PathManager.getResourceRoot(Job::class.java, "/kotlinx/coroutines/Job.class")

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                    classpath += listOf(jvmStdlibPath!!, coroutinesPath!!)
                }
            }
        }

        testInline(
            """
            /src/com/sample/MyJob.kt
            package com.sample
            import kotlinx.coroutines.Job
            abstract class MyJob: Job
            """.trimIndent(),
            configuration
        ) {
            lateinit var provider: ExternalDocumentablesProvider
            pluginsSetupStage = {
                provider = it.plugin<DokkaBase>().querySingle { externalDocumentablesProvider }
            }
            documentablesTransformationStage = { mod ->
                val entry = mod.packages.single().classlikes.single().cast<DClass>().supertypes.entries.single()
                val res = provider.findClasslike(
                    entry.value.single().typeConstructor.dri,
                    entry.key)
                assertEquals("Job", res?.name)
                assertEquals("kotlinx.coroutines/Job///PointingToDeclaration/", res?.dri?.toString())

                val supertypes = res?.cast<DInterface>()?.supertypes?.values?.single()
                    ?.map { it.typeConstructor.dri.classNames }
                assertEquals(
                    listOf("CoroutineContext.Element"),
                    supertypes
                )
                assertEquals(Language.KOTLIN, res!!.sources.values.single().language)
                assertEquals("KotlinBuiltins.kt", res.sources.values.single().path)
            }
        }
    }

    @Test
    fun `external documentable for nested class`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src")
                    analysisPlatform = "jvm"
                    classpath += jvmStdlibPath!!
                }
            }
        }

        testInline(
            """
            /src/com/sample/MyList.kt
            package com.sample
            abstract class MyEntry: Map.Entry<Int, String>
            """.trimIndent(),
            configuration
        ) {
            lateinit var provider: ExternalDocumentablesProvider
            pluginsSetupStage = {
                provider = it.plugin<DokkaBase>().querySingle { externalDocumentablesProvider }
            }
            documentablesTransformationStage = { mod ->
                val entry = mod.packages.single().classlikes.single().cast<DClass>().supertypes.entries.single()
                val res = provider.findClasslike(
                    entry.value.single().typeConstructor.dri,
                    entry.key)
                assertEquals("Entry", res?.name)
                assertEquals("kotlin.collections/Map.Entry///PointingToDeclaration/", res?.dri?.toString())
                assertEquals(Language.KOTLIN, res!!.sources.values.single().language)
                assertEquals("KotlinBuiltins.kt", res.sources.values.single().path)
            }
        }
    }
}
