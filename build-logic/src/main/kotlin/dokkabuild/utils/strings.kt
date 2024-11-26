/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild.utils

/** Title case the first char of a string */
fun String.uppercaseFirstChar(): String = replaceFirstChar(Char::uppercaseChar)
