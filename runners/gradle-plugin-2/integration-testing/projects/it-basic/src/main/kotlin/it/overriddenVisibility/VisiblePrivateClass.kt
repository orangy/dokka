package it.overriddenVisibility

/**
 * Private classes and methods generally should not be visible, but [documentedVisibilities]
 * are overriden for this specific package to include private code
 *
 * §PRIVATE§ (marker for asserts)
 */
private class VisiblePrivateClass {
    private val privateVal: Int = 0
    private fun privateMethod() {}
}