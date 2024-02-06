/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class WasmJsWasiTestedVersionsArgumentsProvider : AllSupportedTestedVersionsArgumentsProvider() {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return super.provideArguments(context).filter {
            val buildVersions = it.get().single() as BuildVersions
            buildVersions.kotlinVersion >= "1.9.20" // 1.9.20 is the first public version that can be tested with wasm-js and wasm-wasi
        }
    }
}

class WasmJsWasiGradleIntegrationTest : AbstractGradleIntegrationTest() {

    @BeforeTest
    fun prepareProjectFiles() {
        val templateProjectDir = File("projects", "it-wasm-js-wasi-basic")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .filterNot { it.name == "local.properties" }
            .filterNot { it.name.startsWith("gradlew") }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @OnlyDescriptors
    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(WasmJsWasiTestedVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(buildVersions, "dokkaHtml", "-s").buildRelaxed()
        assertEquals(TaskOutcome.SUCCESS, assertNotNull(result.task(":dokkaHtml")).outcome)

        val htmlOutputDir = File(projectDir, "build/dokka/html")
        assertTrue(htmlOutputDir.isDirectory, "Missing html output directory")

        assertTrue(
            htmlOutputDir.allHtmlFiles().count() > 0,
            "Expected html files in html output directory"
        )

        htmlOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoUnresolvedLinks(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }
    }
}
