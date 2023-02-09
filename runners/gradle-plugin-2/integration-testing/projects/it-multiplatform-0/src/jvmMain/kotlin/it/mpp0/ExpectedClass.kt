package it.mpp0

actual class ExpectedClass {
    actual val platform: String = "jvm"

    /**
     * This function can only be used by JVM consumers
     */
    fun jvmOnlyFunction() = Unit

}
