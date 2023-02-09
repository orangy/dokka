package it.protected

/**
 * Protected class should be visible because it's included in documentedVisibilities
 *
 * §PROTECTED§ (marker for asserts)
 */
protected class ProtectedClass {
    protected fun protectedFun(): String = "protected"
}
