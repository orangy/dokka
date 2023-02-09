import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.kotlinSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.DokkaConfiguration
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:${System.getenv("DOKKA_VERSION")}")
    }
}

apply(from = "../template.root.gradle.kts")

dependencies {
    implementation(kotlin("stdlib"))
}

fun createTask(name: String) {
    tasks.register(name, org.jetbrains.dokka.gradle.DokkaTask::class) {
        dokkaSourceSets {
            moduleName.set("Some example")
            register("kotlin-stdlib-common") {
                sourceRoots.from("src/main/java")
                sourceRoots.from("src/main/kotlin")
                samples.from("src/main/kotlin")
            }
        }
    }
}

task("runTasks") {
    val taskNumber = (properties["task_number"] as String).toInt()
    repeat(taskNumber) { i ->
        createTask("task_"+i)
        dependsOn ("task_"+i)
    }
}

