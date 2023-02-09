@file:UseSerializers(
    FileAsPathStringSerializer::class,
    DokkaSourceSetIDSerializer::class,
    URLSerializer::class,
)

package org.jetbrains.dokka.gradle.dokka_configuration

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.gradle.api.Named
import org.jetbrains.dokka.*
import java.io.File
import java.net.URL
import java.nio.file.Paths


// Implementations of DokkaConfiguration interfaces that can be serialized to files.
// Serialization is required because Gradle tasks can only pass data to one-another via files.


@Serializable
data class DokkaConfigurationKxs(
    override val moduleName: String,
    override val moduleVersion: String? = null,
    override val outputDir: File,
    override val cacheRoot: File? = null,
    override val offlineMode: Boolean,
    override val failOnWarning: Boolean,
    override val sourceSets: List<DokkaSourceSetKxs>,
    override val pluginsClasspath: List<File>,
    override val pluginsConfiguration: List<PluginConfigurationKxs>,
    override val delayTemplateSubstitution: Boolean,
    override val suppressObviousFunctions: Boolean,
    override val includes: Set<File>,
    override val suppressInheritedMembers: Boolean,
    override val finalizeCoroutines: Boolean,

    val modulesKxs: List<DokkaModuleDescriptionKxs>,
) : DokkaConfiguration, Named {

    override val modules: List<DokkaConfiguration.DokkaModuleDescription> =
        modulesKxs.map { it.toCoreModel(outputDir) }

    override fun getName(): String = moduleName

    @Serializable
    data class DokkaSourceSetKxs(
        override val sourceSetID: DokkaSourceSetID,
        override val displayName: String,
        override val classpath: List<File>,
        override val sourceRoots: Set<File>,
        override val dependentSourceSets: Set<DokkaSourceSetID>,
        override val samples: Set<File>,
        override val includes: Set<File>,
        override val reportUndocumented: Boolean,
        override val skipEmptyPackages: Boolean,
        override val skipDeprecated: Boolean,
        override val jdkVersion: Int,
        override val sourceLinks: Set<SourceLinkDefinitionKxs>,
        override val perPackageOptions: List<PackageOptionsKxs>,
        override val externalDocumentationLinks: Set<ExternalDocumentationLinkKxs>,
        override val languageVersion: String? = null,
        override val apiVersion: String? = null,
        override val noStdlibLink: Boolean,
        override val noJdkLink: Boolean,
        override val suppressedFiles: Set<File>,
        override val analysisPlatform: Platform,
        override val documentedVisibilities: Set<DokkaConfiguration.Visibility>,
    ) : DokkaConfiguration.DokkaSourceSet, Named {

        override fun getName(): String = displayName

        @Deprecated("see DokkaConfiguration.DokkaSourceSet.includeNonPublic")
        override val includeNonPublic: Boolean = DokkaDefaults.includeNonPublic
    }


    @Serializable
    data class SourceLinkDefinitionKxs(
        override val localDirectory: String,
        override val remoteUrl: URL,
        override val remoteLineSuffix: String? = null,
    ) : DokkaConfiguration.SourceLinkDefinition


    @Serializable
    data class PackageOptionsKxs(
        override val matchingRegex: String,
        override val reportUndocumented: Boolean? = null,
        override val skipDeprecated: Boolean,
        override val suppress: Boolean,
        override val documentedVisibilities: Set<DokkaConfiguration.Visibility>,
    ) : DokkaConfiguration.PackageOptions {

        @Deprecated("see DokkaConfiguration.PackageOptions.includeNonPublic")
        override val includeNonPublic: Boolean = DokkaDefaults.includeNonPublic
    }


    @Serializable
    data class PluginConfigurationKxs(
        override val fqPluginName: String,
        override val serializationFormat: DokkaConfiguration.SerializationFormat,
        // a string that might contain escaped JSON/XML
        override val values: String,
    ) : DokkaConfiguration.PluginConfiguration


    /**
     * Note: this class implements [java.io.Serializable] because it is used as a
     * [Gradle Property][org.gradle.api.provider.Property], and Gradle must be able to fingerprint
     * property values classes using Java Serialization.
     *
     * All other configuration data classes also implement [java.io.Serializable] via their parent interfaces.
     */
    @Serializable
    data class DokkaModuleDescriptionKxs(
        /** @see DokkaConfiguration.DokkaModuleDescription.name */
        val moduleName: String,
        /** @see DokkaConfiguration.DokkaModuleDescription.sourceOutputDirectory */
        val sourceOutputDirectory: File,
        /** @see DokkaConfiguration.DokkaModuleDescription.includes */
        val includes: Set<File>,

//        /**
//         * Not part of the Dokka spec - will be used before Dokka Generation to compute the relative dir
//         * @see toCoreModel
//         */
//        val moduleOutputDirectory: File,
    ) : java.io.Serializable, Named {

        override fun getName(): String = moduleName

        /**
         * Map this Module Description to [DokkaConfiguration.DokkaModuleDescription]
         *
         * This is necessary because [DokkaConfiguration.DokkaModuleDescription] requires a relative path
         * to the root output directory, and this is unknown when a [DokkaModuleDescriptionKxs] is created.
         */
        fun toCoreModel(
            rootOutputDirectory: File
        ): DokkaConfiguration.DokkaModuleDescription {
//            val relativePathToOutputDirectory = moduleOutputDirectory.relativeToOrSelf(rootOutputDirectory)
            val relativePathToOutputDirectory = sourceOutputDirectory.relativeToOrSelf(rootOutputDirectory)

            println("relativePathToOutputDirectory: $relativePathToOutputDirectory")

            return DokkaModuleDescriptionImpl(
                name = name,
                relativePathToOutputDirectory = relativePathToOutputDirectory,
                sourceOutputDirectory = sourceOutputDirectory,
                includes = includes,
            )
        }
    }


    @Serializable
    data class ExternalDocumentationLinkKxs(
        override val url: URL,
        override val packageListUrl: URL,
    ) : DokkaConfiguration.ExternalDocumentationLink
}


/** Serializer for [DokkaSourceSetID] */
private object DokkaSourceSetIDSerializer : KSerializer<DokkaSourceSetID> {

    @Serializable
    private data class DokkaSourceSetIDDelegate(
        val scopeId: String,
        val sourceSetName: String,
    )

    private val delegateSerializer = DokkaSourceSetIDDelegate.serializer()

    override val descriptor: SerialDescriptor = delegateSerializer.descriptor

    override fun deserialize(decoder: Decoder): DokkaSourceSetID {
        val delegate = decoder.decodeSerializableValue(delegateSerializer)
        return DokkaSourceSetID(
            scopeId = delegate.scopeId,
            sourceSetName = delegate.sourceSetName,
        )
    }

    override fun serialize(encoder: Encoder, value: DokkaSourceSetID) {
        val delegate = DokkaSourceSetIDDelegate(
            scopeId = value.scopeId,
            sourceSetName = value.sourceSetName,
        )
        encoder.encodeSerializableValue(delegateSerializer, delegate)
    }
}


/** Serialize a [URL] as string */
private object URLSerializer : KSerializer<URL> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.net.URL", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): URL = URL(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: URL) = encoder.encodeString(value.toString())
}


/**
 * Serialize a [File] as an absolute, canonical file path, with
 * [invariant path separators][invariantSeparatorsPath]
 */
private object FileAsPathStringSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.io.File", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): File =
        Paths.get(decoder.decodeString()).toFile()

    override fun serialize(encoder: Encoder, value: File): Unit =
        encoder.encodeString(value.invariantSeparatorsPath)
}
