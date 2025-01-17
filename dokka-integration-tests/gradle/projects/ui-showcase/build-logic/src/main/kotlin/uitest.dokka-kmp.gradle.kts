/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.AbstractDokkaTask

plugins {
    id("uitest.dokka")
}

tasks.withType<AbstractDokkaTask>().configureEach {
    @Language("JSON")
    val dokkaBaseConfiguration = """
    {
      "footerMessage": "<div class='kt-footer__license'>Kotlin™ is protected under the <a href='https://kotlinlang.org/foundation/kotlin-foundation.html' data-test='external-link ' target='_blank' rel='noreferrer noopener' class='kt-footer__link'>Kotlin&nbsp;Foundation</a> and licensed under the&nbsp;<a href='https://github.com/JetBrains/kotlin-web-site/blob/master/LICENSE' data-test='external-link ' target='_blank' rel='noreferrer noopener' class='kt-footer__link'>Apache&nbsp;2 license</a>. </div>",
      "homepageLink": "https://github.com/Kotlin/dokka/tree/master/dokka-integration-tests/gradle/projects/ui-showcase"
    }
    """.trimIndent()

    pluginsMapConfiguration.set(
        mapOf(
            // fully qualified plugin name to json configuration
            "org.jetbrains.dokka.base.DokkaBase" to dokkaBaseConfiguration
        )
    )
}
