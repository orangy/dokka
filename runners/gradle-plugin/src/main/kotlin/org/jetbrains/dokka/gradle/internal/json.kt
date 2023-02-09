package org.jetbrains.dokka.gradle.internal

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.File
import com.fasterxml.jackson.core.type.TypeReference as JacksonTypeReference


private val objectMapper = run {
    val module = SimpleModule().apply {
        addSerializer(FileSerializer)
    }
    jacksonObjectMapper()
        .registerModule(module)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

internal class TypeReference<T> private constructor(
    internal val jackson: JacksonTypeReference<T>
) {
    companion object {
        internal inline operator fun <reified T> invoke(): TypeReference<T> = TypeReference(jacksonTypeRef())
    }
}

internal fun toJsonString(value: Any): String = objectMapper
    .writerWithDefaultPrettyPrinter()
    .writeValueAsString(value)

internal inline fun <reified T : Any> parseJson(json: String): T = parseJson(json, TypeReference())

internal fun <T : Any> parseJson(json: String, typeReference: TypeReference<T>): T =
    objectMapper.readValue(json, typeReference.jackson)


private object FileSerializer : StdScalarSerializer<File>(File::class.java) {
    override fun serialize(value: File, g: JsonGenerator, provider: SerializerProvider) {
        g.writeString(value.path)
    }
}
