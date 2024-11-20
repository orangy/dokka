/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.junit.jupiter.api.Tag
import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * JUnit [Tag] indicating the test uses Dokka Gradle Plugin in V1 mode.
 */
@Tag("DGPv1")
@Target(FUNCTION, CLASS)
@MustBeDocumented
@Inherited
annotation class TestsDGPv1


/**
 * JUnit [Tag] indicating the test uses Dokka Gradle Plugin in V2 mode.
 */
@Tag("DGPv2")
@Target(FUNCTION, CLASS)
@MustBeDocumented
@Inherited
annotation class TestsDGPv2


/**
 * JUnit [Tag] indicating the test involves an Android project.
 *
 * If a test is annotated with [TestsAndroid] then
 * [DokkaGradlePluginTestExtension] will run the test multiple times,
 * and provide a [DokkaGradleProjectRunner] using [TestedVersions.Android].
 */
@Tag("Android")
@Target(FUNCTION, CLASS)
@MustBeDocumented
@Inherited
annotation class TestsAndroid


/**
 * JUnit [Tag] indicating the test uses Android and Compose.
 *
 * If a test is annotated with [TestsAndroid] then
 * [DokkaGradlePluginTestExtension] will run the test multiple times,
 * and provide a [DokkaGradleProjectRunner] using [TestedVersions.AndroidCompose].
 */
@Tag("Compose")
@TestsAndroid
@Target(FUNCTION, CLASS)
@MustBeDocumented
@Inherited
annotation class TestsAndroidCompose
