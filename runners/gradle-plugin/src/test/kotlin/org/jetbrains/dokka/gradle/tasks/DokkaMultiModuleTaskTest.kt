@file:Suppress("UnstableApiUsage")

package org.jetbrains.dokka.gradle.tasks

import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.*
import java.io.File
import kotlin.test.*
import org.jetbrains.dokka.gradle.*

class DokkaMultiModuleTaskTest {

    private val rootProject = ProjectBuilder.builder()
        .withName("root")
        .build()

    private val childProject = ProjectBuilder.builder()
        .withName("child")
        .withProjectDir(rootProject.projectDir.resolve("child"))
        .withParent(rootProject).build()

    private val childDokkaTask = childProject.tasks.create<DokkaTaskPartial>("childDokkaTask")

    private val multiModuleTask = rootProject.tasks.create<DokkaMultiModuleTask>("multiModuleTask").apply {
        addChildTask(childDokkaTask)
    }

    init {
        rootProject.plugins.apply("org.jetbrains.dokka")
        childProject.plugins.apply("org.jetbrains.dokka")

        rootProject.allprojects {
            tasks.withType<AbstractDokkaTask>().configureEach {
                plugins.withDependencies { clear() }
            }
        }
    }

    @Test
    fun `child project is withing root project`() {
        assertEquals(
            rootProject.projectDir, childProject.projectDir.parentFile,
            "Expected child project being inside the root project"
        )

        assertEquals(
            childProject.projectDir.name, "child",
            "Expected folder of child project to be called 'child'"
        )
    }

    @Test
    fun buildDokkaConfiguration() {
        val include1 = childDokkaTask.project.file("include1.md")
        val include2 = childDokkaTask.project.file("include2.md")
        val topLevelInclude = multiModuleTask.project.file("README.md")

        childDokkaTask.apply {
            dokkaSourceSets.create("main")
            dokkaSourceSets.create("test")
            dokkaSourceSets.configureEach {
                includes.from(include1, include2)
            }
        }

        multiModuleTask.apply {
            moduleVersion.set("1.5.0")
            moduleName.set("custom Module Name")
            outputDirectory.set(project.buildDir.resolve("customOutputDirectory"))
            cacheRoot.set(File("customCacheRoot"))
            pluginsConfiguration.add(
                PluginConfigurationImpl(
                    "pluginA",
                    DokkaConfiguration.SerializationFormat.JSON,
                    """ { "key" : "value2" } """
                )
            )
            failOnWarning.set(true)
            offlineMode.set(true)
            includes.from(listOf(topLevelInclude))
            finalizeCoroutines.set(true)
        }

        val dokkaConfiguration = multiModuleTask.buildDokkaConfiguration()
        assertEquals(
            DokkaConfigurationImpl(
                moduleName = "custom Module Name",
                moduleVersion = "1.5.0",
                outputDir = multiModuleTask.project.buildDir.resolve("customOutputDirectory"),
                cacheRoot = File("customCacheRoot"),
                pluginsConfiguration = mutableListOf(
                    PluginConfigurationImpl(
                        "pluginA",
                        DokkaConfiguration.SerializationFormat.JSON,
                        """ { "key" : "value2" } """
                    )
                ),
                pluginsClasspath = emptyList(),
                failOnWarning = true,
                offlineMode = true,
                includes = setOf(topLevelInclude),
                modules = listOf(
                    DokkaModuleDescriptionImpl(
                        name = "child",
                        relativePathToOutputDirectory = File("child"),
                        includes = setOf(include1, include2),
                        sourceOutputDirectory = childDokkaTask.outputDirectory.get()
                    )
                ),
                finalizeCoroutines = true,
            ),
            dokkaConfiguration
        )
    }

    @Test
    fun `multimodule task should not include unspecified version`() {
        childDokkaTask.apply {
            dokkaSourceSets.create("main")
            dokkaSourceSets.create("test")
        }

        multiModuleTask.apply {
            moduleVersion.set("unspecified")
        }

        val dokkaConfiguration = multiModuleTask.buildDokkaConfiguration()
        assertNull(dokkaConfiguration.moduleVersion)
    }

    @Test
    fun `setting dokkaTaskNames declares proper task dependencies`() {
        val dependenciesInitial = multiModuleTask.taskDependencies.getDependencies(multiModuleTask).toSet()
        assertEquals(1, dependenciesInitial.size, "Expected one dependency")
        val dependency = dependenciesInitial.single()

        assertTrue(
            dependency is DokkaTaskPartial,
            "Expected dependency to be of Type ${DokkaTaskPartial::class.simpleName}"
        )
        assertEquals(childProject, dependency.project, "Expected dependency from child project")


        val customDokkaTask = childProject.tasks.create<DokkaTask>("customDokkaTask")

        multiModuleTask.addSubprojectChildTasks("customDokkaTask")
        val dependenciesAfter = multiModuleTask.taskDependencies.getDependencies(multiModuleTask).toSet()

        assertEquals(2, dependenciesAfter.size, "Expected two dependencies")
        assertTrue(customDokkaTask in dependenciesAfter, "Expected 'customDokkaTask' in dependencies")

    }

    @Test
    fun `multimodule task with no child tasks throws DokkaException`() {
        val project = ProjectBuilder.builder().build()
        val multimodule = project.tasks.create<DokkaMultiModuleTask>("multimodule")
        project.configurations.configureEach { withDependencies { clear() } }
        assertFailsWith<DokkaException> { multimodule.generateDocumentation() }
    }

    @Test
    fun childDokkaTaskIncludes() {
        val childDokkaTaskInclude1 = childProject.file("include1")
        val childDokkaTaskInclude2 = childProject.file("include2")
        val childDokkaTaskInclude3 = childProject.file("include3")

        childDokkaTask.apply {
            dokkaSourceSets.create("main") {
                includes.from(childDokkaTaskInclude1, childDokkaTaskInclude2)
            }
            dokkaSourceSets.create("main2") {
                includes.from(childDokkaTaskInclude3)
            }
        }

        val secondChildDokkaTaskInclude = childProject.file("include4")
        val secondChildDokkaTask = childProject.tasks.create<DokkaTaskPartial>("secondChildDokkaTask") {
            dokkaSourceSets.create("main") {
                includes.from(secondChildDokkaTaskInclude)
            }
        }
        multiModuleTask.addChildTask(secondChildDokkaTask)

        assertEquals(
            mapOf(
                ":child:childDokkaTask" to setOf(
                    childDokkaTaskInclude1,
                    childDokkaTaskInclude2,
                    childDokkaTaskInclude3
                ),
                ":child:secondChildDokkaTask" to setOf(secondChildDokkaTaskInclude)
            ),
            multiModuleTask.childDokkaTaskIncludes
        )
    }

    @Test
    fun sourceChildOutputDirectories() {
        val parent = ProjectBuilder.builder().build()
        val child = ProjectBuilder.builder().withName("child").withParent(parent).build()

        val parentTask = parent.tasks.create<DokkaMultiModuleTask>("parent")
        val childTask = child.tasks.create<DokkaTask>("child")

        parentTask.addChildTask(childTask)
        childTask.outputDirectory.set(child.file("custom/output"))

        assertEquals(
            listOf(parent.file("child/custom/output")), parentTask.sourceChildOutputDirectories,
            "Expected child output directory being present"
        )
    }

    @Test
    fun targetChildOutputDirectories() {
        val parent = ProjectBuilder.builder().build()
        val child = ProjectBuilder.builder().withName("child").withParent(parent).build()

        val parentTask = parent.tasks.create<DokkaMultiModuleTask>("parent")
        val childTask = child.tasks.create<DokkaTask>("child")

        parentTask.addChildTask(childTask)

        parentTask.fileLayout.set(DokkaMultiModuleFileLayout { taskParent, taskChild ->
            taskParent.project.buildDir.resolve(taskChild.name)
        })

        assertEquals(
            listOf(parent.project.buildDir.resolve("child")), parentTask.targetChildOutputDirectories,
            "Expected child target output directory being present"
        )
    }
}
