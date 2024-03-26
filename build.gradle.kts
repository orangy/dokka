/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_TASK_GROUP
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP

plugins {
    id("dokkabuild.base")
}

//region Workarounds for running all tasks in included builds
// https://github.com/gradle/gradle/issues/22335
// See build-logic/src/main/kotlin/dokkabuild.base.gradle.kts
fun Task.dependsOnIncludedBuildTasks(
    taskName: String = name
) {
    description = "Lifecycle task that runs '$taskName' in all included builds"
    dependsOn("includedBuildTasks_$taskName")
}

val publishPlugins by tasks.registering {
    group = "gradle plugin"
    dependsOnIncludedBuildTasks()
}

val validatePlugins by tasks.registering {
    group = "gradle plugin"
    dependsOnIncludedBuildTasks()
}

val apiDump by tasks.registering {
    group = "$VERIFICATION_GROUP bcv"
    dependsOnIncludedBuildTasks()
}

val apiCheck by tasks.registering {
    group = "$VERIFICATION_GROUP bcv"
    dependsOnIncludedBuildTasks()
}

val test by tasks.registering {
    group = VERIFICATION_GROUP
    dependsOnIncludedBuildTasks()
    dependsOn(apiCheck)
}

val integrationTest by tasks.registering {
    group = VERIFICATION_GROUP
    dependsOnIncludedBuildTasks()
    description = "Runs integration tests of this project. Might take a while and require additional setup."
}

val publishAllPublicationsToRemoteRepositories by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks("publishAllPublicationsToMavenCentralRepository")
    dependsOnIncludedBuildTasks("publishAllPublicationsToProjectLocalRepository")
    dependsOnIncludedBuildTasks("publishAllPublicationsToSnapshotRepository")
    dependsOnIncludedBuildTasks("publishAllPublicationsToSpaceDevRepository")
}
val publishAllPublicationsToMavenCentralRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishAllPublicationsToProjectLocalRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishAllPublicationsToSnapshotRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishAllPublicationsToSpaceDevRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishAllPublicationsToSpaceTestRepository by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
val publishToMavenLocal by tasks.registering {
    group = PUBLISH_TASK_GROUP
    dependsOnIncludedBuildTasks()
}
//endregion
