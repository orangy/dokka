/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild.utils

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Fetch the example directories in `dokka/examples/gradle-v2` using a [ValueSource].
 *
 * A [ValueSource] is necessary to ensure that Gradle registers the directory values as Configuration Cache inputs,
 * but doesn't become overly sensitive to the contents of the directories.
 */
abstract class DokkaGradleExampleDirectoriesSource :
    ValueSource<List<File>, DokkaGradleExampleDirectoriesSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        /**
         * The directory containing the Dokka Gradle v2 examples: `dokka/examples/gradle-v2`
         */
        val exampleGradleProjectsDir: DirectoryProperty
    }

    override fun obtain(): List<File> {
        return parameters.exampleGradleProjectsDir.get().asFile.toPath()
            .listDirectoryEntries()
            .filter { it.isDirectory() && it.name.endsWith("-example") }
            .map { it.toFile() }
    }
}
