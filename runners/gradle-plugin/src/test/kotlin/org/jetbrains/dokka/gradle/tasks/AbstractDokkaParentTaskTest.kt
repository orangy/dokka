package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.gradle.tasks.AbstractDokkaParentTask
import org.jetbrains.dokka.gradle.tasks.DokkaTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AbstractDokkaParentTaskTest {

    private val rootProject = ProjectBuilder.builder().build()
    private val subproject0 = ProjectBuilder.builder().withName("subproject0").withParent(rootProject).build()
    private val subproject1 = ProjectBuilder.builder().withName("subproject1").withParent(rootProject).build()
    private val subSubproject0 = ProjectBuilder.builder().withName("subSubproject0").withParent(subproject0).build()

    init {
        rootProject.subprojects {
            tasks.create<DokkaTask>("dokkaTask")
        }
    }

    private val parentTask = rootProject.tasks.create<TestDokkaParentTask>("parent")


    @Test
    fun `add and remove tasks by reference`() {
        assertEquals(
            emptySet(), parentTask.childDokkaTasks,
            "Expected no childDokkaTasks by default"
        )

        parentTask.addChildTask(subproject0.dokkaTask)
        assertEquals(
            setOf(subproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subproject0.dokkaTask.path} being registered as child task"
        )

        parentTask.addChildTask(subproject1.dokkaTask)
        assertEquals(
            setOf(subproject0.dokkaTask, subproject1.dokkaTask), parentTask.childDokkaTasks,
            "Expected both dokka tasks being present"
        )

        parentTask.removeChildTask(subproject0.dokkaTask)
        assertEquals(
            setOf(subproject1.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subproject0.dokkaTask.path} being removed from child tasks"
        )

        parentTask.addChildTask(subSubproject0.dokkaTask)
        assertEquals(
            setOf(subproject1.dokkaTask, subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subSubproject0.dokkaTask.path} being added as child task"
        )

        parentTask.addChildTask(subSubproject0.dokkaTask)
        assertEquals(
            setOf(subproject1.dokkaTask, subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected no effect for adding a task twice"
        )
    }

    @Test
    fun `add and remove by absolute path`() {
        parentTask.addChildTask(":subproject0:dokkaTask")
        assertEquals(
            setOf(subproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subproject0.dokkaTask.path} as child task"
        )

        parentTask.addChildTask(":subproject0:subSubproject0:dokkaTask")
        assertEquals(
            setOf(subproject0.dokkaTask, subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subSubproject0.dokkaTask.path} being added as child task"
        )

        parentTask.removeChildTask(":subproject0:dokkaTask")
        assertEquals(
            setOf(subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subproject0.dokkaTask.path} being removed as child task"
        )
    }

    @Test
    fun `add and remove by relative path`() {
        parentTask.addChildTask("subproject0:dokkaTask")
        assertEquals(
            setOf(subproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subproject0.dokkaTask.path} as child task"
        )

        parentTask.addChildTask("subproject0:subSubproject0:dokkaTask")
        assertEquals(
            setOf(subproject0.dokkaTask, subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subSubproject0.dokkaTask.path} being added as child task"
        )

        parentTask.removeChildTask("subproject0:dokkaTask")
        assertEquals(
            setOf(subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subproject0.dokkaTask.path} being removed as child task"
        )
    }

    @Test
    fun `add and remove by relative path ob subproject0`() {
        val parentTask = subproject0.tasks.create<TestDokkaParentTask>("parent")

        parentTask.addChildTask("subSubproject0:dokkaTask")
        assertEquals(
            setOf(subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subSubproject0.dokkaTask.path} being registered as child"
        )

        parentTask.removeChildTask("subSubproject0:dokkaTask")
        assertEquals(
            emptySet(), parentTask.childDokkaTasks,
            "Expected ${subSubproject0.dokkaTask.path} being removed as child"
        )
    }

    @Test
    fun `add and remove by project and name`() {
        parentTask.addChildTasks(rootProject.subprojects, "dokkaTask")
        assertEquals(
            setOf(subproject0.dokkaTask, subproject1.dokkaTask, subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected all subproject tasks being registered as child task"
        )

        parentTask.removeChildTasks(rootProject.subprojects, "dokkaTask")
        assertEquals(
            emptySet(), parentTask.childDokkaTasks,
            "Expected all tasks being removed"
        )

        parentTask.addChildTasks(listOf(subproject0), "dokkaTask")
        assertEquals(
            setOf(subproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected only ${subproject0.dokkaTask.path} being registered as child"
        )

        parentTask.addSubprojectChildTasks("dokkaTask")
        assertEquals(
            setOf(subproject0.dokkaTask, subproject1.dokkaTask, subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected all subproject tasks being registered as child task"
        )

        parentTask.removeSubprojectChildTasks("dokkaTask")
        assertEquals(
            emptySet(), parentTask.childDokkaTasks,
            "Expected all tasks being removed"
        )

        parentTask.addSubprojectChildTasks("dokkaTask")
        assertEquals(
            setOf(subproject0.dokkaTask, subproject1.dokkaTask, subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected all subproject tasks being registered as child task"
        )

        parentTask.removeChildTasks(subproject0)
        assertEquals(
            setOf(subproject1.dokkaTask, subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected only ${subproject0.dokkaTask.path} being removed"
        )

        parentTask.addSubprojectChildTasks("dokkaTask")
        parentTask.removeChildTasks(listOf(subproject0, subproject1))
        assertEquals(
            setOf(subSubproject0.dokkaTask), parentTask.childDokkaTasks,
            "Expected ${subproject0.dokkaTask.path} and ${subproject1.dokkaTask.path} being removed"
        )
    }

    @Test
    fun `adding invalid path will not throw exception`() {
        parentTask.addChildTask(":some:stupid:path")
        parentTask.childDokkaTasks
    }

    @Test
    fun `adding non dokka task will throw exception`() {
        val badTask = rootProject.tasks.create("badTask")
        parentTask.addChildTask(badTask.path)
        assertFailsWith<IllegalArgumentException> { parentTask.childDokkaTasks }
    }
}

internal abstract class TestDokkaParentTask : AbstractDokkaParentTask() {
    override fun buildDokkaConfiguration(): DokkaConfigurationImpl {
        throw NotImplementedError()
    }
}

private val Project.dokkaTask: DokkaTask get() = tasks.getByName<DokkaTask>("dokkaTask")
