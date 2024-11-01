/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild.utils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dokkaBuild
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// We must use Kotlin language/api version 1.4 (DEPRECATED) to support Gradle 7,
// and to do that we need to use old Kotlin version, which has support for such an old Kotlin version (e.g., 2.0.20)
fun Project.configureGradleKotlinCompatibility() {
    if (!dokkaBuild.enforceGradleKotlinCompatibility.get()) return

    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerVersion.set("2.0.20")
        coreLibrariesVersion = "2.0.20"
        compilerOptions {
            languageVersion.set(KotlinVersion.fromVersion("1.4"))
            apiVersion.set(KotlinVersion.fromVersion("1.4"))
            freeCompilerArgs.add("-Xsuppress-version-warnings")
        }
    }
}
