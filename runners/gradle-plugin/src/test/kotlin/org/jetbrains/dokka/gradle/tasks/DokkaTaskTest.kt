package org.jetbrains.dokka.gradle.tasks

import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaDefaults.suppress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.jetbrains.dokka.gradle.*

class DokkaTaskTest {
    @Test
    fun `no suppressed source sets are present after in built configuration`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        val task = project.tasks.create<DokkaTask>("dokkaTask")
        project.configurations.all { withDependencies { clear() } }

        task.dokkaSourceSets.register("main")
        task.dokkaSourceSets.register("jvm")
        task.dokkaSourceSets.register("test") {
            suppress.set(true)
        }

        assertEquals(
            listOf("main", "jvm").sorted(),
            task.buildDokkaConfiguration().sourceSets.map { it.sourceSetID.sourceSetName }.sorted(),
            "Expected only unsuppressed source sets `main` and `test` to be present in built configuration"
        )
    }

    @Test
    fun `module version is not present if not specified`(){
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka")

        val task = project.tasks.create<DokkaTask>("dokkaTask")
        project.configurations.all { withDependencies { clear() } }

        task.dokkaSourceSets.register("main")
        assertNull(task.buildDokkaConfiguration().moduleVersion)
    }
}
