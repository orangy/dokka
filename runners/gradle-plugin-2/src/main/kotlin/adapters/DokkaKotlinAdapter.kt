package org.jetbrains.dokka.gradle.adapters

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.internal.not
import org.jetbrains.dokka.gradle.tasks.DokkaConfigurationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.io.File
import javax.inject.Inject

/**
 * The [DokkaKotlinAdapter] plugin will automatically register Kotlin source sets as Dokka source sets.
 */
abstract class DokkaKotlinAdapter @Inject constructor(
    private val providers: ProviderFactory,
) : Plugin<Project> {

    private val logger = Logging.getLogger(this::class.java)

    override fun apply(project: Project) {
        logger.lifecycle("applied DokkaKotlinAdapter to ${project.path}")

        project.pluginManager.apply {
//            withPlugin("kotlin") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-dsl") { registerKotlinSourceSets(project) }
//            withPlugin("embedded-kotlin") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-android") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-android-extensions") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-kapt") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-multiplatform") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-native-cocoapods") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-native-performance") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-parcelize") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-platform-android") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-platform-common") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-platform-js") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-platform-jvm") { registerKotlinSourceSets(project) }
//            withPlugin("kotlin-scripting") { registerKotlinSourceSets(project) }
//            withPlugin("org.jetbrains.kotlin.android.extensions") { registerKotlinSourceSets(project) }
            withPlugin("org.jetbrains.kotlin.android") { exec(project) }
            withPlugin("org.jetbrains.kotlin.js") { exec(project) }
            withPlugin("org.jetbrains.kotlin.jvm") { exec(project) }
//            withPlugin("org.jetbrains.kotlin.kapt") { registerKotlinSourceSets(project) }
//            withPlugin("org.jetbrains.kotlin.multiplatform.pm20") { registerKotlinSourceSets(project) }
            withPlugin("org.jetbrains.kotlin.multiplatform") { exec(project) }
//            withPlugin("org.jetbrains.kotlin.native.cocoapods") { registerKotlinSourceSets(project) }
//            withPlugin("org.jetbrains.kotlin.native.performance") { registerKotlinSourceSets(project) }
//            withPlugin("org.jetbrains.kotlin.platform.android") { registerKotlinSourceSets(project) }
//            withPlugin("org.jetbrains.kotlin.platform.common") { registerKotlinSourceSets(project) }
//            withPlugin("org.jetbrains.kotlin.platform.js") { registerKotlinSourceSets(project) }
//            withPlugin("org.jetbrains.kotlin.platform.jvm") { registerKotlinSourceSets(project) }
//            withPlugin("org.jetbrains.kotlin.plugin.parcelize") { registerKotlinSourceSets(project) }
//            withPlugin("org.jetbrains.kotlin.plugin.scripting") { registerKotlinSourceSets(project) }
        }
    }

    private fun exec(project: Project) {
        val kotlinExtension = project.extensions.findKotlinExtension()
        if (kotlinExtension == null) {
            logger.lifecycle("could not find Kotlin Extension")
            return
        }
        logger.lifecycle("Configuring Dokka in Gradle Kotlin Project ${project.path}")

        val dokka = project.extensions.getByType<DokkaExtension>()

        val kotlinAdapterContext = KotlinAdapterContext(
            dokka,
            kotlinExtension,
            project.configurations,
            providers,
        )

        with(kotlinAdapterContext) {

            kotlinExtension.sourceSets.all kss@{
                logger.lifecycle("auto configuring Kotlin Source Set $name")

                registerKotlinSourceSet(this@kss)

                dokka.dokkaSourceSets.all dss@{
                    if (this@kss.name == this@dss.name) {
                        logger.lifecycle("setting suppress convention for SourceSet $name")
                        suppress.convention(!this@kss.isMainSourceSet())
                    }
                }
            }

            // TODO remove this - tasks shouldn't be configured directly, instead source sets
            //      should be added to the Dokka extension, which will then create and configure appropriate tasks
            project.tasks.withType<DokkaConfigurationTask>().configureEach {
                dokkaSourceSets.configureEach {
                    suppress.convention(
                        todoSourceSetName
                            .flatMap { kotlinExtension.sourceSets.named(it).flatMap { kss -> !kss.isMainSourceSet() } }
                    )
                }
            }
        }
    }

    class KotlinAdapterContext(
        val dokka: DokkaExtension,
        private val kotlinExtension: KotlinProjectExtension,
        private val projectConfigurations: ConfigurationContainer,
        private val providers: ProviderFactory,
    ) {
        private val logger = Logging.getLogger(this::class.java)

        /** Determine if a source set is 'main', and not test sources */
        fun KotlinSourceSet.isMainSourceSet(): Provider<Boolean> = providers.provider {
            val currentSourceSet = this

            val allCompilations = when (kotlinExtension) {
                is KotlinMultiplatformExtension -> {
                    kotlinExtension.targets
                        .flatMap { target -> target.compilations }

                }

                is KotlinSingleTargetExtension<*> -> {
                    kotlinExtension.target.compilations
                }

                else -> emptyList()
            }.filter { compilation ->
                currentSourceSet == compilation.defaultSourceSet
                        || currentSourceSet in compilation.kotlinSourceSets
                        || currentSourceSet in compilation.allKotlinSourceSets
            }

            fun KotlinCompilation<*>.isMainCompilation(): Boolean {
                return try {
                    when (this) {
                        is KotlinJvmAndroidCompilation ->
                            androidVariant is LibraryVariant || androidVariant is ApplicationVariant

                        else ->
                            name == "main"
                    }
                } catch (e: NoSuchMethodError) {
                    // Kotlin Plugin version below 1.4
                    !name.toLowerCase().endsWith("test")
                }
            }

            logger.lifecycle("KotlinSourceSet $name. empty: ${allCompilations.isEmpty()}. main compilations: ${allCompilations.count { it.isMainCompilation() }}")

            allCompilations.isEmpty() || allCompilations.any { compilation -> compilation.isMainCompilation() }
        }

        /** Determine the platform(s) that the Kotlin Plugin is targeting */
        private val kotlinTarget: Provider<KotlinPlatformType> = providers.provider {
            when (kotlinExtension) {
                is KotlinMultiplatformExtension ->
                    kotlinExtension.targets
                        .map { it.platformType }
                        .singleOrNull()
                        ?: KotlinPlatformType.common

                is KotlinSingleTargetExtension<*> ->
                    kotlinExtension.target.platformType

                else -> KotlinPlatformType.common
            }
        }

        private val dokkaAnalysisPlatform: Provider<Platform> =
            kotlinTarget.map { target -> Platform.fromString(target.name) }


        fun registerKotlinSourceSet(kotlinSourceSet: KotlinSourceSet) {

            // TODO: Needs to respect filters.
            //  We probably need to change from "sourceRoots" to support "sourceFiles"
            //  https://github.com/Kotlin/dokka/issues/1215
            val extantKotlinSourceRoots = kotlinSourceSet.kotlin.sourceDirectories.filter { it.exists() }

            logger.lifecycle("kotlin source set ${kotlinSourceSet.name} has source roots: ${extantKotlinSourceRoots.map { it.invariantSeparatorsPath }}")

            dokka.dokkaSourceSets.register(kotlinSourceSet.name) {
                this.suppress.set(!kotlinSourceSet.isMainSourceSet())

                this.sourceRoots.from(extantKotlinSourceRoots)

                // need to check for resolution, because testImplementation can't be resolved....
                // > Resolving dependency configuration 'testImplementation' is not allowed as it is defined as 'canBeResolved=false'.
                // >    Instead, a resolvable ('canBeResolved=true') dependency configuration that extends 'testImplementation' should be resolved.
                // As a workaround, just manually check if the configuration can be resolved.
                // If resolution is necessary, maybe make a special, one-off, resolvable configuration?
                this.classpath.from(
                    projectConfigurations
                        .named(kotlinSourceSet.implementationConfigurationName)
                        .map { elements ->
                            when {
                                elements.isCanBeResolved ->
                                    elements.incoming.artifactView { lenient(true) }.files

                                else ->
                                    emptySet<File>()
                            }
                        }
                )

                this.analysisPlatform.set(dokkaAnalysisPlatform)

                kotlinSourceSet.dependsOn.forEach {
                    // TODO remove dependentSourceSets.size workaround
                    this.dependentSourceSets.register(it.name + dependentSourceSets.size) {
                        this.sourceSetName = it.name
                    }
                }

                this.displayName.set(kotlinTarget.map { target ->
                    kotlinSourceSet.name.substringBeforeLast(
                        delimiter = "Main",
                        missingDelimiterValue = target.name
                    )
                })
            }
        }
    }

    companion object {

        private fun ExtensionContainer.findKotlinExtension(): KotlinProjectExtension? =
            try {
                findByType()
                    ?: findByType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>()
            } catch (e: Throwable) {
                when (e) {
                    is TypeNotPresentException,
                    is ClassNotFoundException,
                    is NoClassDefFoundError -> null

                    else -> throw e
                }
            }
    }
}
