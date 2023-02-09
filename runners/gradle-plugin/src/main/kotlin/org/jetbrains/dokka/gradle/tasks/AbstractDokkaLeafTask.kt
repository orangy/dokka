package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.kotlin.dsl.container
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.dokka.gradle.*
import org.jetbrains.dokka.gradle.checkSourceSetDependencies
import org.jetbrains.dokka.gradle.kotlinOrNull

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class AbstractDokkaLeafTask : AbstractDokkaTask() {

    @get:Internal
    val dokkaSourceSets: NamedDomainObjectContainer<GradleDokkaSourceSetBuilder> =
        project.container(GradleDokkaSourceSetBuilder::class, gradleDokkaSourceSetBuilderFactory()).also { container ->
            DslObject(this).extensions.add("dokkaSourceSets", container)
            project.kotlinOrNull?.sourceSets?.all sourceSet@{
                container.register(name) {
                    configureWithKotlinSourceSet(this@sourceSet)
                }
            }
        }

    /**
     * Only contains source sets that are marked with `isDocumented`.
     * Non documented source sets are not relevant for Gradle's UP-TO-DATE mechanism, as well
     * as task dependency graph.
     */
    @get:Nested
    protected val unsuppressedSourceSets: List<GradleDokkaSourceSetBuilder>
        get() = dokkaSourceSets
            .toList()
            .also(::checkSourceSetDependencies)
            .filterNot { it.suppress.get() }
}
