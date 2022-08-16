package model

import org.jetbrains.dokka.base.transformers.documentables.CallableExtensions
import org.jetbrains.dokka.model.*
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import org.jetbrains.dokka.model.properties.WithExtraProperties

class ExtensionsTest : AbstractModelTest("/src/main/kotlin/classes/Test.kt", "classes") {
    private fun <T : WithExtraProperties<R>, R : Documentable> T.checkExtension(name: String = "extension") =
        with(extra[CallableExtensions]?.extensions) {
            this notNull "extensions"
            this counts 1
            (this?.single() as? DFunction)?.name equals name
        }

    @Test
    fun extensionForSubclasses() {
        inlineModelTest(
            """
            |open class A
            |open class B: A()
            |open class C: B()
            |open class D: C()
            |fun B.extension() = ""
            """
        ) {
            with((this / "classes" / "B").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "C").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "D").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "A").cast<DClass>()) {
                extra[CallableExtensions] equals null
            }
        }
    }

    @Test
    fun extensionForInterfaces() {
        inlineModelTest(
            """
            |interface I
            |interface I2 : I
            |open class A: I2
            |fun I.extension() = ""
            """
        ) {

            with((this / "classes" / "A").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "I2").cast<DInterface>()) {
                checkExtension()
            }
            with((this / "classes" / "I").cast<DInterface>()) {
                checkExtension()
            }
        }
    }

    @Test
    fun extensionForExternalClasses() {
        inlineModelTest(
            """
            |abstract class A<T>: AbstractList<T>()
            |fun<T> AbstractCollection<T>.extension() {}
            |
            |class B:Exception()
            |fun Throwable.extension() = ""
            """
        ) {
            with((this / "classes" / "A").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "B").cast<DClass>()) {
                checkExtension()
            }
        }
    }

    @Test
    fun extensionForTypeAlias() {
        inlineModelTest(
            """
            |class A {}
            |class B: A {}
            |class C: B {}
            |class D: C {}
            |typealias B2 = B
            |fun B2.extension() = ""
            """
        ) {
            with((this / "classes" / "B").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "C").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "D").cast<DClass>()) {
                checkExtension()
            }
            with((this / "classes" / "A").cast<DClass>()) {
                extra[CallableExtensions] equals null
            }
        }
    }
}