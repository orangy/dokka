/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.dokkatoo.formats

import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi

abstract class DokkatooJekyllPlugin
@DokkatooInternalApi
constructor() : DokkatooFormatPlugin(formatName = "jekyll") {
    override fun DokkatooFormatPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("jekyll-plugin"))
        }
    }
}
