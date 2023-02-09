package it.mpp0

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer

/**
 * Will print the raw value
 */
fun CPointer<CPointed>.customExtension() {
    println(this.rawValue)
}
