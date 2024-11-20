/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.jetbrains.dokka.gradle.utils.GradlePropertiesBuilder
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

/**
 * Add this annotation to test functions or classes to provide more values to set into the
 * `gradle.properties` file of the tested project.
 * The properties will be appended to all tests in the current test context.
 */
@Target(FUNCTION, CLASS)
@Repeatable
@MustBeDocumented
annotation class WithGradleProperties(
    vararg val providers: KClass<out GradlePropertiesProvider> = [],
)

/**
 * Provide Gradle properties to be added to the `gradle.properties` file in the root directory of the tested project.
 *
 * The provided properties will be appended to any existing properties.
 */
fun interface GradlePropertiesProvider {
    fun get(): Map<String, String>

    object Default : GradlePropertiesProvider {
        override fun get(): Map<String, String> {
            return GradlePropertiesBuilder().build()
        }
    }

    object Android : GradlePropertiesProvider {
        override fun get(): Map<String, String> = buildMap {
            put("android.useAndroidX", "true")
        }
    }
}
