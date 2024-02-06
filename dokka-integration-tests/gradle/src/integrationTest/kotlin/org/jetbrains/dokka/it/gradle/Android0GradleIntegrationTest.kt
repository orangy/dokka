/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AndroidTestedVersionsArgumentsProvider : TestedVersionsArgumentsProvider(TestedVersions.ANDROID)

class Android0GradleIntegrationTest : AbstractGradleIntegrationTest() {

    companion object {
        /**
         * Indicating whether or not the current machine executing the test is a CI
         */
        private val isCI: Boolean get() = System.getenv("CI") == "true"

        private val isAndroidSdkInstalled: Boolean = System.getenv("ANDROID_SDK_ROOT") != null ||
                System.getenv("ANDROID_HOME") != null

        fun assumeAndroidSdkInstalled() {
            if (isCI) return
            if (!isAndroidSdkInstalled) {
                throw IllegalStateException("Expected Android SDK to be installed")
            }
        }
    }

    @BeforeTest
    fun prepareProjectFiles() {
        assumeAndroidSdkInstalled()
        val templateProjectDir = File("projects", "it-android-0")

        templateProjectDir.listFiles().orEmpty()
            .filter { it.isFile }
            .filterNot { it.name == "local.properties" }
            .filterNot { it.name.startsWith("gradlew") }
            .forEach { topLevelFile -> topLevelFile.copyTo(File(projectDir, topLevelFile.name)) }

        File(templateProjectDir, "src").copyRecursively(File(projectDir, "src"))
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(AndroidTestedVersionsArgumentsProvider::class)
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
            assertNoUnresolvedLinks(file, knownUnresolvedDRIs)
            assertNoHrefToMissingLocalFileOrDirectory(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }

        assertTrue(
            htmlOutputDir.allHtmlFiles().any { file ->
                "https://developer.android.com/reference/kotlin/android/content/Context.html" in file.readText()
            }, "Expected link to developer.android.com"
        )

        assertTrue(
            htmlOutputDir.allHtmlFiles().any { file ->
                "https://developer.android.com/reference/kotlin/androidx/appcompat/app/AppCompatActivity.html" in
                        file.readText()
            }, "Expected link to developer.android.com/.../androidx/"
        )

        htmlOutputDir.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoHrefToMissingLocalFileOrDirectory(file)
        }
    }

    // TODO: remove this list when https://github.com/Kotlin/dokka/issues/1306 is closed
    private val knownUnresolvedDRIs = setOf(
        "it.android/IntegrationTestActivity/findViewById/#kotlin.Int/PointingToGenericParameters(0)/",
        "it.android/IntegrationTestActivity/getExtraData/#java.lang.Class[TypeParam(bounds=[androidx.core.app.ComponentActivity.ExtraData])]/PointingToGenericParameters(0)/",
        "it.android/IntegrationTestActivity/getSystemService/#java.lang.Class[TypeParam(bounds=[kotlin.Any])]/PointingToGenericParameters(0)/",
        "it.android/IntegrationTestActivity/requireViewById/#kotlin.Int/PointingToGenericParameters(0)/"
    )
}
