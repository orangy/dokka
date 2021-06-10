package org.jetbrains.dokka.base.resolvers.shared

import org.jetbrains.dokka.base.renderers.PackageListService
import java.net.URL

typealias Module = String

data class PackageList(
    val linkFormat: RecognizedLinkFormat,
    val modules: Map<Module, Set<String>>,
    val locations: Map<String, String>,
    val url: URL
) {
    val packages: Set<String>
        get() = modules.values.flatten().toSet()

    fun moduleFor(packageName: String) = modules.asSequence()
            .filter { it.value.contains(packageName) }
            .first().key

    companion object {
        const val MODULE_DELIMITER = "module:"

        fun load(url: URL, jdkVersion: Int, offlineMode: Boolean = false): PackageList? {
            if (offlineMode && url.protocol.toLowerCase() != "file")
                return null

            val packageListStream = runCatching { url.readContent() }.onFailure {
                println("Failed to download package-list from $url, this might suggest that remote resource is not available," +
                        " module is empty or dokka output got corrupted")
                return null
            }.getOrThrow()

            val (params, packages) = packageListStream
                .bufferedReader()
                .useLines { lines -> lines.partition { it.startsWith(PackageListService.DOKKA_PARAM_PREFIX) } }

            val paramsMap = splitParams(params)
            val format = linkFormat(paramsMap["format"]?.singleOrNull(), jdkVersion)
            val locations = splitLocations(paramsMap["location"].orEmpty()).filterKeys(String::isNotEmpty)

            val modulesMap = splitPackages(packages)
            return PackageList(format, modulesMap, locations, url)
        }

        private fun splitParams(params: List<String>) = params.asSequence()
            .map { it.removePrefix("${PackageListService.DOKKA_PARAM_PREFIX}.").split(":", limit = 2) }
            .groupBy({ (key, _) -> key }, { (_, value) -> value })

        private fun splitLocations(locations: List<String>) = locations.map { it.split("\u001f", limit = 2) }
            .map { (key, value) -> key to value }
            .toMap()

        private fun splitPackages(packages: List<String>): Map<Module, Set<String>> {
            var lastModule: Module = ""

            return packages.fold(mutableMapOf()) { acc, el ->
                if (el.startsWith(MODULE_DELIMITER)) {
                    lastModule = el.substringAfter(MODULE_DELIMITER)
                } else if(el.isNotBlank()) {
                    acc[lastModule] = acc.getOrDefault(lastModule, emptySet()) + el
                }
                acc
            }
        }

        private fun linkFormat(formatName: String?, jdkVersion: Int) =
            formatName?.let { RecognizedLinkFormat.fromString(it) }
                ?: when {
                    jdkVersion < 8 -> RecognizedLinkFormat.Javadoc1 // Covers JDK 1 - 7
                    jdkVersion < 10 -> RecognizedLinkFormat.Javadoc8 // Covers JDK 8 - 9
                    else -> RecognizedLinkFormat.Javadoc10 // Covers JDK 10+
                }
    }
}
