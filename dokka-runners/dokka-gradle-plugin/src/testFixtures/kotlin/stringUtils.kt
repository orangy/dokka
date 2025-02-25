/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils


fun String.splitToPair(delimiter: String): Pair<String, String> =
    substringBefore(delimiter) to substringAfter(delimiter)


/** Title case the first char of a string */
fun String.uppercaseFirstChar(): String = mapFirstChar(Character::toTitleCase)


private inline fun String.mapFirstChar(
    transform: (Char) -> Char
): String = if (isNotEmpty()) transform(this[0]) + substring(1) else this


/** Split a string into lines, sort the lines, and re-join them (using [separator]). */
fun String.sortLines(separator: String = "\n") =
    lines()
        .sorted()
        .joinToString(separator)


/** Replace characters that don't match [isLetterOrDigit] with [replacement]. */
fun String.replaceNonAlphaNumeric(
    replacement: String = "-"
): String =
    asIterable().joinToString("") { c ->
        if (c.isLetterOrDigit()) "$c" else replacement
    }
