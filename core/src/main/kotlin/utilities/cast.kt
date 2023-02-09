package org.jetbrains.dokka.utilities

inline fun <reified T> Any.cast(): T = this as T
