/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/** Edit environment variables in the Gradle Runner */
@Deprecated("Windows does not support withEnvironment - https://github.com/gradle/gradle/issues/23959")
fun GradleRunner.withEnvironment(build: MutableMap<String, String?>.() -> Unit): GradleRunner {
    val env = environment ?: mutableMapOf()
    env.build()
    return withEnvironment(env)
}


@OptIn(ExperimentalContracts::class)
inline fun GradleRunner.build(
    handleResult: BuildResult.() -> Unit
) {
    contract { callsInPlace(handleResult, InvocationKind.EXACTLY_ONCE) }
    build().let(handleResult)
}



@OptIn(ExperimentalContracts::class)
inline fun GradleRunner.buildAndFail(
    handleResult: BuildResult.() -> Unit
) {
    contract { callsInPlace(handleResult, InvocationKind.EXACTLY_ONCE) }
    buildAndFail().let(handleResult)
}


/**
 * Create, or overwrite, the `gradle.properties` file in [GradleRunner.getProjectDir].
 */
fun GradleRunner.writeGradleProperties(
    arguments: GradlePropertiesBuilder,
): GradleRunner {
    val gradlePropertiesFile = projectDir.resolve("gradle.properties").apply {
        if (!exists()) {
            parentFile.mkdirs()
            createNewFile()
        }
    }

    val gradleProperties = arguments.build()

    // Avoid using java.util.Properties because it produces non-deterministic output (e.g. a timestamp),
    // which negatively impacts caching.

    gradlePropertiesFile.writeText(
        gradleProperties
            .entries
            .sortedBy { it.key }
            .joinToString("\n", postfix = "\n") { (k, v) -> "$k=$v" }
    )

    return this
}


/**
 * Helper function to _append_ [arguments] to any existing
 * [GradleRunner arguments][GradleRunner.getArguments].
 */
fun GradleRunner.addArguments(
    vararg arguments: String
): GradleRunner = addArguments(arguments.asList())

/**
 * Helper function to _append_ [arguments] to any existing
 * [GradleRunner arguments][GradleRunner.getArguments].
 */
fun GradleRunner.addArguments(
    arguments: List<String>,
): GradleRunner =
    withArguments(this@addArguments.arguments + arguments)

/**
 * Get the name of the task, without the leading [BuildTask.getPath].
 */
val BuildTask.name: String
    get() = path.substringAfterLast(':')
