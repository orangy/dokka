package model

import org.jetbrains.dokka.links.sureClassNames
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.KotlinModifier.*
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.assertNotNull
import utils.name
import utils.supers


class ClassesTest : AbstractModelTest("/src/main/kotlin/classes/Test.kt", "classes") {

    @Test
    fun emptyClass() {
        inlineModelTest(
            """
            |class Klass {}"""
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 4
            }
        }
    }

    @Test
    fun emptyObject() {
        inlineModelTest(
            """
            |object Obj {}
            """
        ) {
            with((this / "classes" / "Obj").cast<DObject>()) {
                name equals "Obj"
                children counts 3
            }
        }
    }

    @Test
    fun classWithConstructor() {
        inlineModelTest(
            """
            |class Klass(name: String)
        """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 4

                with(constructors.firstOrNull().assertNotNull("Constructor")) {
                    visibility.values allEquals KotlinVisibility.Public
                    parameters counts 1
                    with(parameters.firstOrNull().assertNotNull("Constructor parameter")) {
                        name equals "name"
                        type.name equals "String"
                    }
                }

            }
        }
    }

    @Test
    fun classWithFunction() {
        inlineModelTest(
            """
            |class Klass {
            |   fun fn() {}
            |}
            """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 5

                with((this / "fn").cast<DFunction>()) {
                    type.name equals "Unit"
                    parameters counts 0
                    visibility.values allEquals KotlinVisibility.Public
                }
            }
        }
    }

    @Test
    fun classWithProperty() {
        inlineModelTest(
            """
            |class Klass {
            |   val name: String = ""
            |}
            """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 5

                with((this / "name").cast<DProperty>()) {
                    name equals "name"
                    // TODO property name
                }
            }
        }
    }

    @Test
    fun classWithCompanionObject() {
        inlineModelTest(
            """
            |class Klass() {
            |   companion object {
            |        val x = 1
            |        fun foo() {}
            |    }
            |}
            """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                children counts 5

                with((this / "Companion").cast<DObject>()) {
                    name equals "Companion"
                    children counts 5

                    with((this / "x").cast<DProperty>()) {
                        name equals "x"
                    }

                    with((this / "foo").cast<DFunction>()) {
                        name equals "foo"
                        parameters counts 0
                        type.name equals "Unit"
                    }
                }
            }
        }
    }

    @Test
    fun dataClass() {
        inlineModelTest(
            """
                |data class Klass() {}
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                visibility.values allEquals KotlinVisibility.Public
                with(extra[AdditionalModifiers].assertNotNull("Extras")) {
                    content counts 1
                    content.first() equals ExtraModifiers.KotlinOnlyModifiers.Data
                }
            }
        }
    }

    @Test
    fun sealedClass() {
        inlineModelTest(
            """
                |sealed class Klass() {}
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                modifier.values.forEach { it equals Sealed }
            }
        }
    }

    @Test
    fun annotatedClassWithAnnotationParameters() {
        inlineModelTest(
            """
                |@Deprecated("should no longer be used") class Foo() {}
                """
        ) {
            with((this / "classes" / "Foo").cast<DClass>()) {
                with(extra[Annotations].assertNotNull("Annotations")) {
                    this.content counts 1
                    with(content.first()) {
                        dri.classNames equals "Deprecated"
                        params.entries counts 1
                        (params["message"].assertNotNull("message") as StringValue).value equals "\"should no longer be used\""
                    }
                }
            }
        }
    }

    @Test
    fun notOpenClass() {
        inlineModelTest(
            """
                |open class C() {
                |    open fun f() {}
                |}
                |
                |class D() : C() {
                |    override fun f() {}
                |}
                """
        ) {
            val C = (this / "classes" / "C").cast<DClass>()
            val D = (this / "classes" / "D").cast<DClass>()

            with(C) {
                modifier.values.forEach { it equals Open }
                with((this / "f").cast<DFunction>()) {
                    modifier.values.forEach { it equals Open }
                }
            }
            with(D) {
                modifier.values.forEach { it equals Final }
                with((this / "f").cast<DFunction>()) {
                    modifier.values.forEach { it equals Open }
                }
                D.supertypes.flatMap { it.component2() }.firstOrNull() equals C.dri
            }
        }
    }

    @Test
    fun indirectOverride() {
        inlineModelTest(
            """
                |abstract class C() {
                |    abstract fun foo()
                |}
                |
                |abstract class D(): C()
                |
                |class E(): D() {
                |    override fun foo() {}
                |}
                """
        ) {
            val C = (this / "classes" / "C").cast<DClass>()
            val D = (this / "classes" / "D").cast<DClass>()
            val E = (this / "classes" / "E").cast<DClass>()

            with(C) {
                modifier.values.forEach { it equals Abstract }
                ((this / "foo").cast<DFunction>()).modifier.values.forEach { it equals Abstract }
            }

            with(D) {
                modifier.values.forEach { it equals Abstract }
            }

            with(E) {
                modifier.values.forEach { it equals Final }

            }
            D.supers.firstOrNull() equals C.dri
            E.supers.firstOrNull() equals D.dri
        }
    }

    @Test
    fun innerClass() {
        inlineModelTest(
            """
                |class C {
                |    inner class D {}
                |}
                """
        ) {
            with((this / "classes" / "C").cast<DClass>()) {

                with((this / "D").cast<DClass>()) {
                    with(extra[AdditionalModifiers].assertNotNull("AdditionalModifiers")) {
                        content counts 1
                        content.first() equals ExtraModifiers.KotlinOnlyModifiers.Inner
                    }
                }
            }
        }
    }

    @Test
    fun companionObjectExtension() {
        inlineModelTest(
            """
                |class Klass {
                |    companion object Default {}
                |}
                |
                |/**
                | * The def
                | */
                |val Klass.Default.x: Int get() = 1
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"

                with((this / "Default").cast<DObject>()) {
                    name equals "Default"
                    // TODO extensions
                }
            }
        }
    }

//    @Test fun companionObjectExtension() {
//        checkSourceExistsAndVerifyModel("testdata/classes/companionObjectExtension.kt", defaultModelConfig) { model ->
//            val pkg = model.members.single()
//            val cls = pkg.members.single { it.name == "Foo" }
//            val extensions = cls.extensions.filter { it.kind == NodeKind.CompanionObjectProperty }
//            assertEquals(1, extensions.size)
//        }
//    }

    @Test
    fun secondaryConstructor() {
        inlineModelTest(
            """
                |class C() {
                |    /** This is a secondary constructor. */
                |    constructor(s: String): this() {}
                |}
                """
        ) {
            with((this / "classes" / "C").cast<DClass>()) {
                name equals "C"
                constructors counts 2

                constructors.map { it.name } allEquals "<init>"

                with(constructors.find { it.parameters.isNullOrEmpty() } notNull "C()") {
                    parameters counts 0
                }

                with(constructors.find { it.parameters.isNotEmpty() } notNull "C(String)") {
                    parameters counts 1
                    with(parameters.firstOrNull() notNull "Constructor parameter") {
                        name equals "s"
                        type.name equals "String"
                    }
                }
            }
        }
    }

    @Test
    fun sinceKotlin() {
        inlineModelTest(
            """
                |/**
                | * Useful
                | */
                |@SinceKotlin("1.1")
                |class C
                """
        ) {
            with((this / "classes" / "C").cast<DClass>()) {
                with(extra[Annotations].assertNotNull("Annotations")) {
                    this.content counts 1
                    with(content.first()) {
                        dri.classNames equals "SinceKotlin"
                        params.entries counts 1
                        (params["version"].assertNotNull("version") as StringValue).value equals "\"1.1\""
                    }
                }
            }
        }
    }

    @Test
    fun privateCompanionObject() {
        inlineModelTest(
            """
                |class Klass {
                |    private companion object {
                |        fun fn() {}
                |        val a = 0
                |    }
                |}
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                assertNull(companion, "Companion should not be visible by default")
            }
        }
    }

    @Test
    fun companionObject() {
        inlineModelTest(
            """
                |class Klass {
                |    companion object {
                |        fun fn() {}
                |        val a = 0
                |    }
                |}
                """
        ) {
            with((this / "classes" / "Klass").cast<DClass>()) {
                name equals "Klass"
                with((this / "Companion").cast<DObject>()) {
                    name equals "Companion"
                    visibility.values allEquals KotlinVisibility.Public

                    with((this / "fn").cast<DFunction>()) {
                        name equals "fn"
                        parameters counts 0
                        receiver equals null
                    }
                }
            }
        }
    }

    @Test
    fun annotatedClass() {
        inlineModelTest(
            """@Suppress("abc") class Foo() {}"""
        ) {
            with((this / "classes" / "Foo").cast<DClass>()) {
                with(extra[Annotations]?.content?.firstOrNull().assertNotNull("annotations")) {
                    dri.toString() equals "kotlin/Suppress///PointingToDeclaration/"
                    (params["names"].assertNotNull("param") as ArrayValue).value equals listOf(StringValue("\"abc\""))
                }
            }
        }
    }

    @Test
    fun javaAnnotationClass() {
        inlineModelTest(
            """
                |import java.lang.annotation.Retention
                |import java.lang.annotation.RetentionPolicy
                |
                |@Retention(RetentionPolicy.SOURCE)
                |public annotation class throws()
            """
        ) {
            with((this / "classes" / "throws").cast<DAnnotation>()) {
                with(extra[Annotations].assertNotNull("Annotations")) {
                    content counts 1
                    with(content.first()) {
                        dri.classNames equals "Retention"
//                        params["value"].assertNotNull("value") equals "(java/lang/annotation/RetentionPolicy, SOURCE)"
                    }
                }
            }
        }
    }

    @Test fun genericAnnotationClass() {
        inlineModelTest(
            """annotation class Foo<A,B,C,D:Number>() {}"""
        ) {
            with((this / "classes" / "Foo").cast<DAnnotation>()){
                generics.map { it.name to it.bounds.first().name } equals listOf("A" to "Any", "B" to "Any", "C" to "Any", "D" to "Number")
            }
        }
    }

    @Test fun nestedGenericClasses(){
        inlineModelTest(
            """
            |class Outer<OUTER> {
            |   inner class Inner<INNER, T : OUTER> { }
            |}
        """.trimMargin()
        ){
            with((this / "classes" / "Outer").cast<DClass>()){
                val inner = classlikes.single().cast<DClass>()
                inner.generics.map { it.name to it.bounds.first().name } equals listOf("INNER" to "Any", "T" to "OUTER")
            }
        }
    }

    @Test fun allImplementedInterfaces() {
        inlineModelTest(
            """
                | interface Highest { }
                | open class HighestImpl: Highest { }
                | interface Lower { }
                | interface LowerImplInterface: Lower { }
                | class Tested : HighestImpl(), LowerImplInterface { }
            """.trimIndent()
        ){
            with((this / "classes" / "Tested").cast<DClass>()){
                extra[ImplementedInterfaces]?.interfaces?.map { it.sureClassNames }?.sorted() equals listOf("Highest", "Lower", "LowerImplInterface").sorted()
            }
        }
    }
}