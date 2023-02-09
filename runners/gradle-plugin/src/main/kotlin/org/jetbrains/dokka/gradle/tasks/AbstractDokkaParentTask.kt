package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.dokka.gradle.parsePath

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AbstractDokkaParentTask : AbstractDokkaTask() {

    @get:Internal
    internal var childDokkaTaskPaths: Set<String> = emptySet()
        private set

    @get:Nested
    internal val childDokkaTasks: Set<AbstractDokkaTask>
        get() = childDokkaTaskPaths
            .mapNotNull { path -> project.tasks.findByPath(path) }
            .map(::checkIsAbstractDokkaTask)
            .toSet()

    /* By task reference */
    fun addChildTask(task: AbstractDokkaTask) {
        childDokkaTaskPaths = childDokkaTaskPaths + task.path
    }

    fun removeChildTask(task: AbstractDokkaTask) {
        childDokkaTaskPaths = childDokkaTaskPaths - task.path
    }

    /* By path */
    fun addChildTask(path: String) {
        childDokkaTaskPaths = childDokkaTaskPaths + project.absoluteProjectPath(path)
    }

    fun removeChildTask(path: String) {
        childDokkaTaskPaths = childDokkaTaskPaths - project.absoluteProjectPath(path)
    }

    /* By project reference and name */
    fun addChildTasks(projects: Iterable<Project>, childTasksName: String) {
        projects.forEach { project ->
            addChildTask(project.absoluteProjectPath(childTasksName))
        }
    }

    fun removeChildTasks(projects: Iterable<Project>, childTasksName: String) {
        projects.forEach { project ->
            removeChildTask(project.absoluteProjectPath(childTasksName))
        }
    }

    fun addSubprojectChildTasks(childTasksName: String) {
        addChildTasks(project.subprojects, childTasksName)
    }

    fun removeSubprojectChildTasks(childTasksName: String) {
        removeChildTasks(project.subprojects, childTasksName)
    }

    fun removeChildTasks(project: Project) {
        childDokkaTaskPaths = childDokkaTaskPaths.filter { path ->
            parsePath(path).parent != parsePath(project.path)
        }.toSet()
    }

    fun removeChildTasks(projects: Iterable<Project>) {
        projects.forEach { project -> removeChildTasks(project) }
    }

    private fun checkIsAbstractDokkaTask(task: Task): AbstractDokkaTask {
        if (task is AbstractDokkaTask) {
            return task
        }
        throw IllegalArgumentException(
            "Only tasks of type ${AbstractDokkaTask::class.java.name} can be added as child for " +
                    "${AbstractDokkaParentTask::class.java.name} tasks.\n" +
                    "Found task ${task.path} of type ${task::class.java.name} added to $path"
        )
    }
}
