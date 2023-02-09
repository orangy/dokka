package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.jetbrains.dokka.gradle.DokkaPlugin

/** Base Dokka task */
@CacheableTask
abstract class DokkaTask : DefaultTask() {

    init {
        group = DokkaPlugin.TASK_GROUP
    }

}
