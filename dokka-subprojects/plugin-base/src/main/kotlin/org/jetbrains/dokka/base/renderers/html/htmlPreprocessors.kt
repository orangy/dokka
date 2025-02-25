/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.templating.AddToSourcesetDependencies
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.transformers.pages.PageTransformer

public open class NavigationPageInstaller(
    public val context: DokkaContext
) : NavigationDataProvider(context), PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode =
        input.modified(
            children = input.children + NavigationPage(
                root = navigableChildren(input),
                moduleName = context.configuration.moduleName,
                context = context
            )
        )
}

public class CustomResourceInstaller(
    public val dokkaContext: DokkaContext
) : PageTransformer {
    private val configuration = configuration<DokkaBase, DokkaBaseConfiguration>(dokkaContext)

    private val customAssets = configuration?.customAssets?.map {
        RendererSpecificResourcePage("images/${it.name}", emptyList(), RenderingStrategy.Copy(it.absolutePath))
    }.orEmpty()

    private val customStylesheets = configuration?.customStyleSheets?.map {
        RendererSpecificResourcePage("styles/${it.name}", emptyList(), RenderingStrategy.Copy(it.absolutePath))
    }.orEmpty()

    override fun invoke(input: RootPageNode): RootPageNode {
        val customResourcesPaths = (customAssets + customStylesheets).map { it.name }.toSet()
        val withEmbeddedResources =
            input.transformContentPagesTree { it.modified(embeddedResources = it.embeddedResources + customResourcesPaths) }
        if(dokkaContext.configuration.delayTemplateSubstitution)
            return withEmbeddedResources
        val (currentResources, otherPages) = withEmbeddedResources.children.partition { it is RendererSpecificResourcePage }
        return input.modified(children = otherPages + currentResources.filterNot { it.name in customResourcesPaths } + customAssets + customStylesheets)
    }
}

public class ScriptsInstaller(private val dokkaContext: DokkaContext) : PageTransformer {

    // scripts ending with `_deferred.js` are loaded with `defer`, otherwise `async`
    private val scriptsPages = listOf(
        "scripts/clipboard.js",
        "scripts/navigation-loader.js",
        "scripts/platform-content-handler.js",
        "scripts/main.js",
        "scripts/prism.js",
        "ui-kit/ui-kit.min.js",

        // It's important for this script to be deferred because it has logic that makes decisions based on
        // rendered elements (for instance taking their clientWidth), and if not all styles are loaded/applied
        // at the time of inspecting them, it will give incorrect results and might lead to visual bugs.
        // should be easy to test if you open any page in incognito or by reloading it (Ctrl+Shift+R)
        "scripts/symbol-parameters-wrapper_deferred.js",
    )

    /**
     * This directory is copied to the output, but is not included in the page
     * header.
     */
    private val copiedScriptDirectories = listOf(
        "scripts/components/"
    )

    override fun invoke(input: RootPageNode): RootPageNode =
        input.let { root ->
            if (dokkaContext.configuration.delayTemplateSubstitution)
                root
            else
                root.modified(children = input.children + (scriptsPages + copiedScriptDirectories).toRenderSpecificResourcePage())
        }.transformContentPagesTree {
            it.modified(
                embeddedResources = it.embeddedResources + scriptsPages
            )
        }
}

public class StylesInstaller(private val dokkaContext: DokkaContext) : PageTransformer {
    private val stylesPages = listOf(
        "styles/style.css",
        "styles/main.css",
        "styles/prism.css",
        "styles/logo-styles.css",
        "styles/font-jb-sans-auto.css",
        "ui-kit/ui-kit.min.css"
    )

    override fun invoke(input: RootPageNode): RootPageNode =
        input.let { root ->
            if (dokkaContext.configuration.delayTemplateSubstitution) root
            else root.modified(children = input.children + stylesPages.toRenderSpecificResourcePage())
        }.transformContentPagesTree {
            it.modified(
                embeddedResources = it.embeddedResources + stylesPages
            )
        }
}

public object AssetsInstaller : PageTransformer {
    private val imagesPages = listOf(
        // ui-kit assets
        "ui-kit/assets/abstract-class-kotlin.svg",
        "ui-kit/assets/abstract-class.svg",
        "ui-kit/assets/annotation-kotlin.svg",
        "ui-kit/assets/annotation.svg",
        "ui-kit/assets/arrow-down.svg",
        "ui-kit/assets/burger.svg",
        "ui-kit/assets/checkbox-off.svg",
        "ui-kit/assets/checkbox-on.svg",
        "ui-kit/assets/class-kotlin.svg",
        "ui-kit/assets/class.svg",
        "ui-kit/assets/cross.svg",
        "ui-kit/assets/enum-kotlin.svg",
        "ui-kit/assets/enum.svg",
        "ui-kit/assets/exception-class.svg",
        "ui-kit/assets/field-value.svg",
        "ui-kit/assets/field-variable.svg",
        "ui-kit/assets/filter.svg",
        "ui-kit/assets/function.svg",
        "ui-kit/assets/go-to-top-icon.svg",
        "ui-kit/assets/homepage.svg",
        "ui-kit/assets/interface-kotlin.svg",
        "ui-kit/assets/interface.svg",
        "ui-kit/assets/object.svg",
        "ui-kit/assets/placeholder.svg",
        "ui-kit/assets/theme-toggle.svg",
        "ui-kit/assets/typealias-kotlin.svg",

        // images
        "images/anchor-copy-button.svg",
        "images/copy-icon.svg",
        "images/copy-successful-icon.svg",
        "images/logo-icon.svg",
    )

    override fun invoke(input: RootPageNode): RootPageNode = input.modified(
        children = input.children + imagesPages.toRenderSpecificResourcePage()
    )
}

private fun List<String>.toRenderSpecificResourcePage(): List<RendererSpecificResourcePage> =
    map { RendererSpecificResourcePage(it, emptyList(), RenderingStrategy.Copy("/dokka/$it")) }

public class SourcesetDependencyAppender(
    public val context: DokkaContext
) : PageTransformer {
    private val name = "scripts/sourceset_dependencies.js"
    override fun invoke(input: RootPageNode): RootPageNode {
        val dependenciesMap = context.configuration.sourceSets.associate {
            it.sourceSetID to it.dependentSourceSets
        }

        fun createDependenciesJson(): String =
            dependenciesMap.map { (key, values) -> key.toString() to values.map { it.toString() } }.toMap()
                .let { content ->
                    if (context.configuration.delayTemplateSubstitution) {
                        toJsonString(AddToSourcesetDependencies(context.configuration.moduleName, content))
                    } else {
                        "sourceset_dependencies='${toJsonString(content)}'"
                    }
                }

        val deps = RendererSpecificResourcePage(
            name = name,
            children = emptyList(),
            strategy = RenderingStrategy.Write(createDependenciesJson())
        )

        return input.modified(
            children = input.children + deps
        ).transformContentPagesTree { it.modified(embeddedResources = it.embeddedResources + name) }
    }
}
