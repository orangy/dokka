/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.formats

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerBinding
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters.Companion.DOKKA_HTML_PARAMETERS_NAME
import org.jetbrains.dokka.gradle.engine.plugins.DokkaVersioningPluginParameters
import org.jetbrains.dokka.gradle.engine.plugins.DokkaVersioningPluginParameters.Companion.DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import org.jetbrains.dokka.gradle.internal.rootGradle
import org.jetbrains.dokka.gradle.internal.uppercaseFirstChar
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import org.jetbrains.dokka.gradle.tasks.LogHtmlPublicationLinkTask
import java.io.File
import javax.inject.Inject

abstract class DokkaHtmlPlugin
@DokkaInternalApi
@Inject
constructor(
    archives: ArchiveOperations,
    providers: ProviderFactory,
) : DokkaFormatPlugin(formatName = "html") {

    private val moduleAggregationCheck: HtmlModuleAggregationCheck =
        HtmlModuleAggregationCheck(archives, providers)

    override fun DokkaFormatPluginContext.configure() {
        registerDokkaBasePluginConfiguration()
        registerDokkaVersioningPlugin()
        configureHtmlUrlLogging()
        configureModuleAggregation()
    }

    private fun DokkaFormatPluginContext.registerDokkaBasePluginConfiguration() {
        with(dokkaExtension.pluginsConfiguration) {
            registerBinding(DokkaHtmlPluginParameters::class, DokkaHtmlPluginParameters::class)
            register<DokkaHtmlPluginParameters>(DOKKA_HTML_PARAMETERS_NAME)
            withType<DokkaHtmlPluginParameters>().configureEach {
                separateInheritedMembers.convention(false)
                mergeImplicitExpectActualDeclarations.convention(false)
            }
        }
    }

    /** register and configure Dokka Versioning Plugin */
    private fun DokkaFormatPluginContext.registerDokkaVersioningPlugin() {
        with(dokkaExtension.pluginsConfiguration) {
            registerBinding(
                DokkaVersioningPluginParameters::class,
                DokkaVersioningPluginParameters::class,
            )
            register<DokkaVersioningPluginParameters>(DOKKA_VERSIONING_PLUGIN_PARAMETERS_NAME)
            withType<DokkaVersioningPluginParameters>().configureEach {
                renderVersionsNavigationOnAllPages.convention(true)
            }
        }
    }

    /** Register a [LogHtmlPublicationLinkTask] task. */
    private fun DokkaFormatPluginContext.configureHtmlUrlLogging() {
        val indexHtmlFile = dokkaTasks.generatePublication
            .flatMap { it.outputDirectory.file("index.html") }

        val indexHtmlPath = indexHtmlFile.map { indexHtml ->
            val rootProjectName = project.rootGradle().rootProject.name
            val rootProjectDir = project.rootGradle().rootProject.projectDir
            val relativePath = indexHtml.asFile.relativeTo(rootProjectDir)
            "${rootProjectName}/${relativePath.invariantSeparatorsPath}"
        }

        val logHtmlUrlTask = project.tasks.register<LogHtmlPublicationLinkTask>(
            "logLink" + dokkaTasks.generatePublication.name.uppercaseFirstChar()
        ) {
            // The default port of IntelliJ's built-in server is defined in the docs
            // https://www.jetbrains.com/help/idea/settings-debugger.html#24aabda8
            // IntelliJ always uses port 63342, but users might configure an additional port.
            this.serverUri.convention("http://localhost:63342")
            this.indexHtmlPath.convention(indexHtmlPath)
        }

        dokkaTasks.generatePublication.configure {
            finalizedBy(logHtmlUrlTask)
        }
    }

    /**
     * - Automatically depend on `all-modules-page-plugin` if aggregating multiple projects.
     * - Add a check that logs a warning if `all-modules-page-plugin` is not defined.
     */
    private fun DokkaFormatPluginContext.configureModuleAggregation() {

        dokkaTasks.generatePublication.configure {
            doFirst("check all-modules-page-plugin is present", moduleAggregationCheck)
        }

        formatDependencies.dokkaPublicationPluginClasspathApiOnly
            .dependencies
            .addLater(dokkaExtension.dokkaEngineVersion.map { v ->
                project.dependencies.create("org.jetbrains.dokka:all-modules-page-plugin:$v")
            })
    }

    /**
     * Log a warning if the publication has 1+ modules but `all-modules-page-plugin` is not present,
     * because otherwise Dokka happily runs and produces no output, which is baffling and unhelpful.
     */
    private class HtmlModuleAggregationCheck(
        private val archives: ArchiveOperations,
        private val providers: ProviderFactory,
    ) : Action<Task> {

        private val checkEnabled: Boolean
            get() = providers
                .gradleProperty(HTML_MODULE_AGGREGATION_CHECK_ENABLED)
                .map(String::toBoolean)
                .getOrElse(true)

        override fun execute(task: Task) {
            if (!checkEnabled) {
                logger.info("[${task.path} ModuleAggregationCheck] check is disabled")
                return
            }

            require(task is DokkaGeneratePublicationTask) {
                "[${task.path} ModuleAggregationCheck] expected DokkaGeneratePublicationTask but got ${task::class}"
            }

            val modulesCount = task.generator.moduleOutputDirectories.count()

            if (modulesCount <= 0) {
                logger.info("[${task.path} ModuleAggregationCheck] skipping check - publication does not have 1+ modules")
                return
            }

            val allDokkaPlugins = task.generator.pluginsClasspath
                .flatMap { file ->
                    extractDokkaPluginMarkers(archives, file)
                }

            val allModulesPagePluginPresent = allDokkaPlugins.any { ALL_MODULES_PAGE_PLUGIN_FQN in it }
            logger.info("[${task.path} ModuleAggregationCheck] allModulesPagePluginPresent:$allModulesPagePluginPresent")

            if (!allModulesPagePluginPresent) {
                val moduleName = task.generator.moduleName.get()

                logger.warn(/* language=text */ """
                    |[${task.path}] org.jetbrains.dokka:all-modules-page-plugin is missing.
                    |
                    |Dokka Publication '$moduleName' has $modulesCount Dokka modules, but
                    |the Dokka Generator plugins classpath does not contain 
                    |   org.jetbrains.dokka:all-modules-page-plugin
                    |which is required for aggregating Dokka HTML modules.
                    |
                    |Dokka Gradle Plugin should have added org.jetbrains.dokka:all-modules-page-plugin automatically.
                    |
                    |Generation will proceed, but the generated output might not contain the full HTML docs.
                    |
                    |Suggestions:
                    | - Verify that the dependency has not been excluded.
                    | - Create an issue with logs, and a reproducer, so we can investigate.
                    |   https://github.com/Kotlin/dokka/
                    |
                    |(all plugins: ${allDokkaPlugins.sorted().joinToString()})
                  """
                    .trimMargin()
                    .prependIndent("> ")
                )
            }
        }
    }

    @DokkaInternalApi
    companion object {
        private val logger = Logging.getLogger(DokkaHtmlPlugin::class.java)

        private const val ALL_MODULES_PAGE_PLUGIN_FQN =
            "org.jetbrains.dokka.allModulesPage.AllModulesPagePlugin"

        private const val HTML_MODULE_AGGREGATION_CHECK_ENABLED =
            "org.jetbrains.dokka.gradle.tasks.html.moduleAggregationCheckEnabled"

        private const val DOKKA_PLUGIN_MARKER_PATH =
            "/META-INF/services/org.jetbrains.dokka.plugability.DokkaPlugin"

        internal fun extractDokkaPluginMarkers(archives: ArchiveOperations, file: File): List<String> {
            val markers = archives.zipTree(file)
                .matching { include(DOKKA_PLUGIN_MARKER_PATH) }

            val pluginIds = markers.flatMap { marker ->
                marker.useLines { lines ->
                    lines
                        .filter { line -> line.isNotBlank() && !line.startsWith("#") }
                        .toList()
                }
            }

            return pluginIds
        }
    }
}
