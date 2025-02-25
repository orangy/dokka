/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.*

public class FileWriter(
    public val context: DokkaContext
): OutputWriter {
    private val createdFiles: MutableSet<String> = mutableSetOf()
    private val createdFilesMutex = Mutex()
    private val jarUriPrefix = "jar:file:"
    private val root = context.configuration.outputDir

    override suspend fun write(path: String, text: String, ext: String) {
        if (checkFileCreated(path)) return

        try {
            val dir = Paths.get(root.absolutePath, path.dropLastWhile { it != '/' }).toFile()
            withContext(Dispatchers.IO) {
                dir.mkdirsOrFail()
                Paths.get(root.absolutePath, "$path$ext").toFile().writeText(text)
            }
        } catch (e: Throwable) {
            context.logger.error("Failed to write $this. ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun checkFileCreated(path: String): Boolean = createdFilesMutex.withLock {
        if (createdFiles.contains(path)) {
            context.logger.error("An attempt to write ${root}/$path several times!")
            return true
        }
        createdFiles.add(path)
        return false
    }

    override suspend fun writeResources(pathFrom: String, pathTo: String) {
        if (javaClass.getResource(pathFrom)?.toURI()?.toString()?.startsWith(jarUriPrefix) == true) {
            copyFromJar(pathFrom, pathTo)
        } else {
            copyFromDirectory(pathFrom, pathTo)
        }
    }


    private suspend fun copyFromDirectory(pathFrom: String, pathTo: String) {
        val dest = Paths.get(root.path, pathTo).toFile()
        val uri = javaClass.getResource(pathFrom)?.toURI()
        val file = uri?.let { File(it) } ?: File(pathFrom)
        withContext(Dispatchers.IO) {
            file.copyRecursively(dest, true)
        }
    }

    private suspend fun copyFromJar(pathFrom: String, pathTo: String) {
        val rebase = fun(path: String) =
            "$pathTo/${path.removePrefix(pathFrom)}"
        val dest = Paths.get(root.path, pathTo).toFile()
        if(dest.isDirectory){
            dest.mkdirsOrFail()
        } else {
            dest.parentFile.mkdirsOrFail()
        }
        val uri = javaClass.getResource(pathFrom).toURI()
        val fs = getFileSystemForURI(uri)
        val path = fs.getPath(pathFrom)
        for (file in Files.walk(path).iterator()) {
            if (Files.isDirectory(file)) {
                val dirPath = file.toAbsolutePath().toString()
                withContext(Dispatchers.IO) {
                    // Append a slash to fix an issue with removePrefix in the rebase function
                    // This causes useless directories to be created in some cases
                    Paths.get(root.path, rebase("$dirPath/")).toFile().mkdirsOrFail()
                }
            } else {
                val filePath = file.toAbsolutePath().toString()
                withContext(Dispatchers.IO) {
                    Paths.get(root.path, rebase(filePath)).toFile().writeBytes(
                        this@FileWriter.javaClass.getResourceAsStream(filePath).use { it?.readBytes() }
                            ?: throw IllegalStateException("Can not get a resource from $filePath")
                    )
                }
            }
        }
    }

    private fun File.mkdirsOrFail() {
        if (!mkdirs() && !exists()) {
            throw IOException("Failed to create directory $this")
        }
    }

    private fun getFileSystemForURI(uri: URI): FileSystem =
        try {
            FileSystems.newFileSystem(uri, emptyMap<String, Any>())
        } catch (e: FileSystemAlreadyExistsException) {
            FileSystems.getFileSystem(uri)
        }
}
