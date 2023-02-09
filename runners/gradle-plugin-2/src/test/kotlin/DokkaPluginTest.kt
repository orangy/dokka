package org.jetbrains.dokka.gradle

import org.gradle.api.Action
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.tasks.DokkaConfigurationTask
import kotlin.test.Test

class DokkaPluginTest {

    @Test
    fun `expect plugin can be applied to project successfully`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka2")
    }

    @Test
    fun `dokka configuration task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka2")

        project.tasks.withType<DokkaConfigurationTask>().configureEach(action {
            dokkaSourceSets.create("Blah", action {

                sourceSetScope.set("moduleName")
                classpath.setFrom(emptyList<String>())
                sourceRoots.from(project.file("src/main/kotlin"))
                samples.from(emptyList<String>())
                includes.from(emptyList<String>())

                //classpath = emptyList()
                //sourceRoots = setOf(file("src/main/kotlin"))
                //dependentSourceSets = emptySet()
                //samples = emptySet()
                //includes = emptySet()
                //documentedVisibilities = DokkaConfiguration.Visibility.values().toSet()
                //reportUndocumented = false
                //skipEmptyPackages = true
                //skipDeprecated = false
                //jdkVersion = 8
                //sourceLinks = emptySet()
                //perPackageOptions = emptyList()
                //externalDocumentationLinks = emptySet()
                //languageVersion = null
                //apiVersion = null
                //noStdlibLink = false
                //noJdkLink = false
                //suppressedFiles = emptySet()
                //analysisPlatform = org.jetbrains.dokka.Platform.DEFAULT
            })
        })
    }
}

@Suppress("ObjectLiteralToLambda") // workaround for https://youtrack.jetbrains.com/issue/KTIJ-14684
private inline fun <T : Any> action(crossinline block: T.() -> Unit) =
    object : Action<T> {
        override fun execute(t: T) = t.block()
    }
