package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.gradle.tasks.AbstractDokkaParentTask

internal fun AbstractDokkaParentTask.checkChildDokkaTasksIsNotEmpty() {
    if (childDokkaTaskPaths.isEmpty()) {
        throw DokkaException(
            """
            The ${this::class.java.simpleName} $path has no configured child tasks. 
            Add some dokka tasks like e.g.: 
            
            tasks.named<AbstractDokkaParentTask>("$name") {
                 addChildTask(..)
                 addChildTasks(subprojects, "...")
                 //...
            }
            """.trimIndent()
        )
    }

    if (childDokkaTasks.isEmpty()) {
        throw DokkaException(
            """
            The ${this::class.java.simpleName} $path could not find any registered child task. 
            child tasks: $childDokkaTaskPaths
            
            Please make sure to apply the dokka plugin to all included (sub)-projects individually e.g.:
            
            // subproject build.gradle.kts
            plugins {
                id("org.jetbrains.dokka")
            }
             
            or 
            
            // parent build.gradle.kts
            subprojects {
                plugins.apply("org.jetbrains.dokka")
            }
            """
        )
    }
}
