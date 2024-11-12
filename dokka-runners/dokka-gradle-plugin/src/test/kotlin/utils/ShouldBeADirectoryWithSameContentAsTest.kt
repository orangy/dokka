/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import io.kotest.assertions.shouldFail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.throwable.shouldHaveMessage
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class ShouldBeADirectoryWithSameContentAsTest : FunSpec({

    test("when expected directory doesn't exist, expect failure") {
        val expectedDir = tempDir()
        val actualDir = tempDir()

        expectedDir.deleteRecursively()

        val failure = shouldFail { expectedDir.shouldBeADirectoryWithSameContentAs(actualDir) }

        failure.shouldHaveMessage("expectedDir '$expectedDir' is not a directory (exists:false, file:false)")
    }

    test("when actual directory doesn't exist, expect failure") {
        val expectedDir = tempDir()
        val actualDir = tempDir()

        actualDir.deleteRecursively()

        val failure = shouldFail { expectedDir.shouldBeADirectoryWithSameContentAs(actualDir) }

        failure.shouldHaveMessage("actualDir '$actualDir' is not a directory (exists:false, file:false)")
    }

    test("when directories have different files, expect failure") {
        val expectedDir = tempDir().apply {
            resolve("file0.txt").writeText("valid file")
            resolve("file1.txt").writeText("not in actual")
            resolve("file2.txt").writeText("not in actual")
            resolve("file3.txt").writeText("not in actual")
        }

        val actualDir = tempDir().apply {
            resolve("file0.txt").writeText("valid file")
            resolve("file-a.txt").writeText("not in expected")
            resolve("file-b.txt").writeText("not in expected")
            resolve("file-c.txt").writeText("not in expected")
        }

        val failure = shouldFail { expectedDir.shouldBeADirectoryWithSameContentAs(actualDir) }

        failure.shouldHaveMessage(
            """
            actualDir is missing 3 files:
              - file1.txt
              - file2.txt
              - file3.txt
            actualDir has 3 unexpected files:
              - file-a.txt
              - file-b.txt
              - file-c.txt
            """.trimIndent()
        )
    }

    test("when file in directories has different content, expect failure with contents of file") {
        val expectedDir = tempDir().apply {
            resolve("file.txt").writeText("content")
        }

        val actualDir = tempDir().apply {
            resolve("file.txt").writeText("unexpected content")
        }

        val failure = shouldFail { expectedDir.shouldBeADirectoryWithSameContentAs(actualDir) }

        failure.shouldHaveMessage(
            """
            file.txt has 1 differences in content:
                --- file.txt
                +++ file.txt
                @@ -1,1 +1,1 @@
                -content
                +unexpected content
            """.trimIndent()
        )
    }

    test("when binary file in directories has different content, expect failure with contents of file") {
        val expectedDir = tempDir().apply {
            resolve("file.bin").writeBytes(PNG_BYTES)
        }

        val actualDir = tempDir().apply {
            resolve("file.bin").writeBytes("This file in the actual dir has valid UTF-8 content".toByteArray())
        }

        val failure = shouldFail { expectedDir.shouldBeADirectoryWithSameContentAs(actualDir) }

        failure.shouldHaveMessage(
            """
            file.bin has 1 differences in content:
                --- file.bin
                +++ file.bin
                @@ -1,4 +1,1 @@
                -Failed to read file content
                -java.nio.charset.MalformedInputException Input length = 1
                -file size: 100
                -checksum: c+g2IzgALWMi8irrW5xr/gB32XVU8WTtaQ8hkD8qXzE=
                +This file in the actual dir has valid UTF-8 content
            """.trimIndent()
        )
    }
})

/**
 * The first 100 bytes from a PNG.
 *
 * Obtained by running `xxd -l 100 -g 1 -u ui-icons_444444_256x240.png`
 *
 * ```
 * 00000000: 89 50 4E 47 0D 0A 1A 0A 00 00 00 0D 49 48 44 52  .PNG........IHDR
 * 00000010: 00 00 01 00 00 00 00 F0 08 04 00 00 00 45 9E 72  .............E.r
 * 00000020: 40 00 00 00 04 67 41 4D 41 00 00 B1 8F 0B FC 61  @....gAMA......a
 * 00000030: 05 00 00 00 20 63 48 52 4D 00 00 7A 26 00 00 80  .... cHRM..z&...
 * 00000040: 84 00 00 FA 00 00 00 80 E8 00 00 75 30 00 00 EA  ...........u0...
 * 00000050: 60 00 00 3A 98 00 00 17 70 9C BA 51 3C 00 00 00  `..:....p..Q<...
 * 00000060: 02 62 4B 47                                      .bKG
 * ```
 */
private val PNG_BYTES: ByteArray =
    intArrayOf(
        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0xF0, 0x08, 0x04, 0x00, 0x00, 0x00, 0x45, 0x9E, 0x72,
        0x40, 0x00, 0x00, 0x00, 0x04, 0x67, 0x41, 0x4D, 0x41, 0x00, 0x00, 0xB1, 0x8F, 0x0B, 0xFC, 0x61,
        0x05, 0x00, 0x00, 0x00, 0x20, 0x63, 0x48, 0x52, 0x4D, 0x00, 0x00, 0x7A, 0x26, 0x00, 0x00, 0x80,
        0x84, 0x00, 0x00, 0xFA, 0x00, 0x00, 0x00, 0x80, 0xE8, 0x00, 0x00, 0x75, 0x30, 0x00, 0x00, 0xEA,
        0x60, 0x00, 0x00, 0x3A, 0x98, 0x00, 0x00, 0x17, 0x70, 0x9C, 0xBA, 0x51, 0x3C, 0x00, 0x00, 0x00,
        0x02, 0x62, 0x4B, 0x47,
    ).map { it.toByte() }.toByteArray()
