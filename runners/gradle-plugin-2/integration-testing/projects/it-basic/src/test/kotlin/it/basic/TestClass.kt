package it.basic

import kotlin.test.Test
import kotlin.test.assertTrue

annotation class OurAnnotation

class TestClass {
    /**
     * Asserts something. [PublicClass]
     */
    @Test
    @OurAnnotation
    fun test() {
        assertTrue(1 == 1)
    }
}
