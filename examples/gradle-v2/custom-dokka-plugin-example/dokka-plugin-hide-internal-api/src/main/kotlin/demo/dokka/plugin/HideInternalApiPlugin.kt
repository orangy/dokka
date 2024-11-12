/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package demo.dokka.plugin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

@Suppress("unused")
class HideInternalApiPlugin : DokkaPlugin() {
  val myFilterExtension by extending {
    plugin<DokkaBase>().preMergeDocumentableTransformer providing ::HideInternalApiTransformer
  }

  @DokkaPluginApiPreview
  override fun pluginApiPreviewAcknowledgement() = PluginApiPreviewAcknowledgement

  companion object {
    const val FQN = "demo.dokka.plugin.HideInternalApiPlugin"
  }
}

/**
 * Configuration for [HideInternalApiPlugin].
 */
@Serializable
data class HideInternalApiConfig(
  val annotatedWith: List<String>
)

class HideInternalApiTransformer(context: DokkaContext) : SuppressedByConditionDocumentableFilterTransformer(context) {

  /**
   * Decode [HideInternalApiPlugin] from the [DokkaContext].
   */
  private val configuration: HideInternalApiConfig by lazy {
    val pluginConfig = context.configuration.pluginsConfiguration
      .firstOrNull { it.fqPluginName == HideInternalApiPlugin.FQN }

    if (pluginConfig != null) {
      require(pluginConfig.serializationFormat == DokkaConfiguration.SerializationFormat.JSON) {
        "HideInternalApiPlugin configuration must be encoded as JSON"
      }

      Json.decodeFromString(HideInternalApiConfig.serializer(), pluginConfig.values)
    } else {
      HideInternalApiConfig(
        annotatedWith = emptyList()
      )
    }
  }

  override fun shouldBeSuppressed(d: Documentable): Boolean {
    val annotations: List<Annotations.Annotation> =
      (d as? WithExtraProperties<*>)
        ?.extra
        ?.allOfType<Annotations>()
        ?.flatMap { it.directAnnotations.values.flatten() }
        ?: emptyList()

    return annotations.any { isInternalAnnotation(it) }
  }

  private fun isInternalAnnotation(annotation: Annotations.Annotation): Boolean {
    val annotationFqn = "${annotation.dri.packageName}.${annotation.dri.classNames}"
    return configuration.annotatedWith.any { it == annotationFqn }
  }
}
