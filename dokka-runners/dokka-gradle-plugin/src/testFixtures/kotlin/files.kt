/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import io.kotest.core.TestConfiguration
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.walk


fun File.copyInto(directory: File, overwrite: Boolean = false) =
    copyTo(directory.resolve(name), overwrite = overwrite)


fun Path.listRelativePathsMatching(predicate: (Path) -> Boolean): List<String> {
    val basePath = this.invariantSeparatorsPathString
    return walk()
        .filter(predicate)
        .map { it.invariantSeparatorsPathString.substringAfter(basePath).removePrefix("/") }
        .toList()
        .sorted()
}

/**
 * Create a temporary directory.
 *
 * Kotest will attempt to delete the file after the current spec has completed.
 *
 * (@see [io.kotest.engine.spec.tempdir], but this returns a [Path], not a [java.io.File].)
 */
fun TestConfiguration.tempDir(prefix: String? = null): Path {
    val dir = createTempDirectory(prefix ?: javaClass.name)
    afterSpec {
        dir.deleteRecursively()
    }
    return dir
}
