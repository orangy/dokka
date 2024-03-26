/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.DokkaBuildProperties
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP

/**
 * A convention plugin that sets up common config and sensible defaults for all subprojects.
 *
 * It provides the [DokkaBuildProperties] extension, for accessing common build properties.
 */

plugins {
    base
}

extensions.create<DokkaBuildProperties>(DokkaBuildProperties.EXTENSION_NAME)

tasks.withType<AbstractArchiveTask>().configureEach {
    // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val integrationTestPreparation by tasks.registering {
    description =
        "lifecycle task for preparing the project for integration tests (for example, publishing to the test Maven repo)"
    group = VERIFICATION_GROUP
}


val subprojectTasksPrefix = "subprojectTasks_"
val includedBuildTasksPrefix = "includedBuildTasks_"

//region Workarounds for running all tasks in included builds
// https://github.com/gradle/gradle/issues/22335
if (project == rootProject) {

    tasks.addRule("Pattern: ${includedBuildTasksPrefix}<TASK>") {
        // Propagate <TASK> to all included builds.
        // (Except build-logic subprojects, because they don't need to be published/tested/etc).
        val taskName = this

        if (startsWith(includedBuildTasksPrefix)) {
            task(taskName) {
                val requestedTaskName = taskName.removePrefix(includedBuildTasksPrefix)
                val includedBuildTasks = gradle.includedBuilds
                    .filter { it.name != "build-logic" }
                    .filter { it.name != "build-settings-logic" }
                    .map { includedBuild ->
                        includedBuild.task(":${subprojectTasksPrefix}${requestedTaskName}")
                    }
                dependsOn(includedBuildTasks)
            }
        }
    }

    tasks.addRule("Pattern: $subprojectTasksPrefix<TASK>") {
        // Run all <TASK>s in the current project's subprojects.
        // This should only be run via the 'includedBuildTasks' rule.
        val taskName = this

        if (startsWith(subprojectTasksPrefix)) {
            task(taskName) {
                val requestedTaskName = taskName.removePrefix(subprojectTasksPrefix)
                val allProjectTasks = subprojects.map { project ->
                    project.tasks.matching { it.name == requestedTaskName }
                }
                dependsOn(allProjectTasks)
            }
        }
    }

    // Setup lifecycle tasks dependencies, so each is propagated to included builds.
    tasks.assemble {
        dependsOn("${includedBuildTasksPrefix}assemble")
    }

    tasks.build {
        dependsOn("${includedBuildTasksPrefix}build")
    }

    tasks.clean {
        dependsOn("${includedBuildTasksPrefix}clean")
    }

    tasks.check {
        dependsOn("${includedBuildTasksPrefix}check")
    }
}
//endregion

val testTasks = tasks.matching { it.name == "test" }
val apiCheckTasks = tasks.matching { it.name == "apiCheck" }

testTasks.configureEach {
    dependsOn(apiCheckTasks)
}
