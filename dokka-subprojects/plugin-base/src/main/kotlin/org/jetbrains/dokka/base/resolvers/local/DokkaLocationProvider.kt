/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.pages.AllTypesPageNode
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import java.util.*

public open class DokkaLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext,
    public val extension: String = ".html"
) : DokkaBaseLocationProvider(pageGraphRoot, dokkaContext) {
    protected open val PAGE_WITH_CHILDREN_SUFFIX: String = "index"

    protected open val pathsIndex: Map<PageNode, List<String>> = IdentityHashMap<PageNode, List<String>>().apply {
        fun registerPath(page: PageNode, prefix: List<String>) {
            if (page is RootPageNode && page.forceTopLevelName) {
                put(page, prefix + PAGE_WITH_CHILDREN_SUFFIX)
                page.children.forEach { registerPath(it, prefix) }
            } else if (page is AllTypesPageNode) {
                put(page, prefix + ALL_TYPES_PAGE_PATH)
                page.children.forEach { registerPath(it, prefix) }
            } else {
                val newPrefix = prefix + page.pathName
                put(page, if (page is ModulePageNode) prefix else newPrefix)
                page.children.forEach { registerPath(it, newPrefix) }
            }

        }
        put(pageGraphRoot, emptyList())
        pageGraphRoot.children.forEach { registerPath(it, emptyList()) }
    }

    protected val pagesIndex: Map<DRIWithSourceSets, ContentPage> =
        pageGraphRoot.withDescendants().filterIsInstance<ContentPage>()
            .flatMap { page ->
                page.dri.flatMap { dri ->
                    page.sourceSets().ifEmpty { setOf(null) }
                        .map { sourceSet -> DRIWithSourceSets(dri, setOfNotNull(sourceSet)) to page }
                        .let {
                            if (it.size > 1) {
                                it + (DRIWithSourceSets(dri, page.sourceSets()) to page)
                            } else {
                                it
                            }
                        }
                }
            }
            .groupingBy { it.first }
            .aggregate { key, _, (_, page), first ->
                if (first) page else throw AssertionError("Multiple pages associated with key: ${key.dri}/${key.sourceSet}")
            }

    @Deprecated("This is not used anymore and will be removed, since resolving references to anchors is removed to fix #3054")
    protected val anchorsIndex: Map<DRIWithSourceSets, PageWithKind> =
        pageGraphRoot.withDescendants().filterIsInstance<ContentPage>()
            .flatMap { page ->
                page.content.withDescendants()
                    .filter { it.extra[SymbolAnchorHint] != null && it.dci.dri.any() }
                    .flatMap { content ->
                        content.dci.dri.map { dri ->
                            (dri to content.sourceSets) to content.extra[SymbolAnchorHint]?.contentKind!!
                        }
                    }
                    .distinct()
                    .flatMap { (pair, kind) ->
                        val (dri, sourceSets) = pair
                        sourceSets.ifEmpty { setOf(null) }.map { sourceSet ->
                            DRIWithSourceSets(dri, setOfNotNull(sourceSet)) to PageWithKind(page, kind)
                        }
                    }
            }.toMap()

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String =
        pathTo(node, context) + if (!skipExtension) extension else ""

    override fun resolve(dri: DRI, sourceSets: Set<DisplaySourceSet>, context: PageNode?): String? =
        sourceSets.ifEmpty { setOf(null) }.mapNotNull { sourceSet ->
            val driWithSourceSets = DRIWithSourceSets(dri, setOfNotNull(sourceSet))
            getLocalLocation(driWithSourceSets, context)
                ?: getLocalLocation(driWithSourceSets.copy(dri = dri.copy(target = PointingToDeclaration)), context)
                // Not found in PageGraph, that means it's an external link
                ?: getExternalLocation(dri, sourceSets)
                ?: getExternalLocation(dri.copy(target = PointingToDeclaration), sourceSets)
        }.distinct().singleOrNull()

    private fun getLocalLocation(driWithSourceSets: DRIWithSourceSets, context: PageNode?): String? {
        val (dri, originalSourceSet) = driWithSourceSets
        val allSourceSets: List<Set<DisplaySourceSet>> =
            listOf(originalSourceSet) + originalSourceSet.let { oss ->
                val ossIds = oss.computeSourceSetIds()
                dokkaContext.configuration.sourceSets.filter { it.sourceSetID in ossIds }
                    .flatMap { it.dependentSourceSets }
                    .mapNotNull { ssid ->
                        dokkaContext.configuration.sourceSets.find { it.sourceSetID == ssid }?.toDisplaySourceSet()
                    }.map {
                        setOf(it)
                    }
            }

        return getLocalPageLink(dri, allSourceSets, context)
    }

    private fun getLocalPageLink(dri: DRI, allSourceSets: Iterable<Set<DisplaySourceSet>>, context: PageNode?)  =
        allSourceSets.mapNotNull { displaySourceSet ->
            pagesIndex[DRIWithSourceSets(dri, displaySourceSet)]
        }.firstOrNull()?.let { page -> resolve(page, context) }

    override fun pathToRoot(from: PageNode): String =
        pathTo(pageGraphRoot, from).removeSuffix(PAGE_WITH_CHILDREN_SUFFIX)

    override fun ancestors(node: PageNode): List<PageNode> =
        generateSequence(node) { it.parent() }.toList()

    protected open fun pathTo(node: PageNode, context: PageNode?): String {
        fun pathFor(page: PageNode) = pathsIndex[page] ?: throw AssertionError(
            "${page::class.simpleName}(${page.name}) does not belong to the current page graph so it is impossible to compute its path"
        )

        val nodePath = pathFor(node)
        val contextPath = context?.let { pathFor(it) }.orEmpty()
        val endedContextPath = if (context?.isIndexPage() == false)
            contextPath.toMutableList().also { it.removeLastOrNull() }
        else contextPath

        val commonPathElements = nodePath.asSequence().zip(endedContextPath.asSequence())
            .takeWhile { (a, b) -> a == b }.count()

        return (List(endedContextPath.size - commonPathElements) { ".." } + nodePath.drop(commonPathElements) +
                if (node.isIndexPage())
                    listOf(PAGE_WITH_CHILDREN_SUFFIX)
                else
                    emptyList()
                ).joinToString("/")
    }

    private fun PageNode.isIndexPage() = this is ClasslikePageNode || children.isNotEmpty()

    private fun PageNode.parent() = pageGraphRoot.parentMap[this]

    private val PageNode.pathName: String
        get() = if (this is PackagePageNode || this is RendererSpecificResourcePage) name else identifierToFilename(name)

    protected data class DRIWithSourceSets(val dri: DRI, val sourceSet: Set<DisplaySourceSet>)

    protected data class PageWithKind(val page: ContentPage, val kind: Kind)

    public companion object {
        private const val ALL_TYPES_PAGE_PATH: String = "all-types"

        public val reservedFilenames: Set<String> = setOf(
            "index", "con", "aux", "lst", "prn", "nul", "eof", "inp", "out",
            ALL_TYPES_PAGE_PATH
        )

        //Taken from: https://stackoverflow.com/questions/1976007/what-characters-are-forbidden-in-windows-and-linux-directory-names
        internal val reservedCharacters = setOf('|', '>', '<', '*', ':', '"', '?', '%')

        public fun identifierToFilename(name: String): String {
            if (name.isEmpty()) return "--root--"
            return sanitizeFileName(name, reservedFilenames, reservedCharacters)
        }
    }
}

internal fun sanitizeFileName(name: String, reservedFileNames: Set<String>, reservedCharacters: Set<Char>): String {
    val lowercase = name.replace("[A-Z]".toRegex()) { matchResult -> "-" + matchResult.value.toLowerCase() }
    val withoutReservedFileNames = if (lowercase in reservedFileNames) "--$lowercase--" else lowercase
    return reservedCharacters.fold(withoutReservedFileNames) { acc, character ->
        if (character in acc) acc.replace(character.toString(), "[${character.toInt()}]")
        else acc
    }
}

