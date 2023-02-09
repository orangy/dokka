@file:Suppress("unused")

package it.basic

import RootPackageClass

/**
 * This class, unlike [RootPackageClass] is located in a sub-package
 */
class PublicClass {
    /**
     * This function is public and documented
     */
    fun publicDocumentedFunction(): String = ""

    fun publicUndocumentedFunction(): String = ""

    /**
     * This function is internal and documented
     */
    internal fun internalDocumentedFunction(): String = ""

    internal fun internalUndocumentedFunction(): String = ""

    /**
     * This function is private and documented
     */
    private fun privateDocumentedFunction(): String = ""

    private fun privateUndocumentedFunction(): String = ""


    /**
     * This property is public and documented
     */
    val publicDocumentedProperty: Int = 0

    val publicUndocumentedProperty: Int = 0

    /**
     * This property internal and documented
     */
    val internalDocumentedProperty: Int = 0

    val internalUndocumentedProperty: Int = 0

    /**
     * This property private and documented
     */
    private val privateDocumentedProperty: Int = 0

    private val privateUndocumentedProperty: Int = 0
}
