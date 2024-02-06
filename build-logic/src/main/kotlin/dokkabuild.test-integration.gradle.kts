/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("dokkabuild.kotlin-jvm")
}

val integrationTestSourceSet: SourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val integrationTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.runtimeOnly.get())
}

/**
 * Dokka's integration test task is not cacheable because the HTML outputs
 * it produces when running the tests are used for showcasing resulting documentation,
 * which does not work well with caching.
 *
 * At the moment there are two problems that do not allow to make it cacheable:
 *
 * 1. The task's inputs are such that changes in Dokka's code do not invalidate the cache,
 *    because it is run with the same version of Dokka (`"DOKKA_VERSION"`) on the same
 *    test project inputs.
 * 2. The tests generate HTML output which is then used to showcase documentation.
 *    The outputs are usually copied to a location from which it will be served.
 *    However, if the test is cacheable, it produces no outputs, so no documentation
 *    to showcase. It needs to be broken into two separate tasks: one cacheable for running
 *    the tests and producing HTML output, and another non-cacheable for copying the output.
 *
 * @see [org.jetbrains.dokka.it.TestOutputCopier] for more details on showcasing documentation
 */
@DisableCachingByDefault(because = "Contains incorrect inputs/outputs configuration, see the KDoc for details")
abstract class NonCacheableIntegrationTest : Test()

val integrationTest by tasks.registering(NonCacheableIntegrationTest::class) {

    // quite high memory because Dokka is a hungry boy
    maxHeapSize = "4g"
    jvmArgs = listOf(
        "-XX:MetaspaceSize=1g",
        "-XX:+HeapDumpOnOutOfMemoryError",
    )

    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath

    useJUnitPlatform {
        if (dokkaBuild.integrationTestUseK2.get()) excludeTags("onlyDescriptors", "onlyDescriptorsMPP")
    }
    // allow inspecting projects in temporary dirs after a test fails
    systemProperty(
        // TODO quick hacks - remove this, will be done in a different PR
        "junit.jupiter.tempdir.cleanup.mode.default",
        if (System.getenv("CI") != null) "ALWAYS" else "ON_SUCCESS",
    )

    systemProperty("org.jetbrains.dokka.experimental.tryK2", dokkaBuild.integrationTestUseK2.get())

    environment("isExhaustive", dokkaBuild.integrationTestExhaustive.get())

    // TODO quick hacks - remove this, will be done in a different PR
    environment("ANDROID_SDK_ROOT",
        providers
            // first try finding a local.properties file in any parent directory
            .provider {
                generateSequence(layout.projectDirectory.asFile, File::getParentFile)
                    .mapNotNull { dir -> dir.resolve("local.properties").takeIf(File::exists) }
                    .flatMap { file -> file.readLines().filter { it.startsWith("sdk.dir=") } }
                    .firstOrNull()
                    ?.substringAfter("sdk.dir=")
            }
            // else try getting pre-installed SDK (e.g. via GitHub step setup-android)
            .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
            .orElse(providers.environmentVariable("ANDROID_HOME"))
            .map(::File).get().invariantSeparatorsPath)

    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
        )
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
