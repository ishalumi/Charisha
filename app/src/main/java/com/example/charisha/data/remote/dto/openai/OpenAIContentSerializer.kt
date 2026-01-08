package com.example.charisha.data.remote.dto.openai

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * OpenAI Content 序列化器
 * 支持纯文本 (String) 和多模态内容 (Array)
 */
object OpenAIContentSerializer : KSerializer<OpenAIContent> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("OpenAIContent")

    override fun serialize(encoder: Encoder, value: OpenAIContent) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is OpenAIContent.Text -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.text))
            is OpenAIContent.Parts -> {
                val json = jsonEncoder.json
                val array = value.parts.map { part ->
                    when (part) {
                        is OpenAIContentPart.Text -> json.encodeToJsonElement(part)
                        is OpenAIContentPart.ImageUrl -> json.encodeToJsonElement(part)
                    }
                }
                jsonEncoder.encodeJsonElement(JsonArray(array))
            }
        }
    }

    override fun deserialize(decoder: Decoder): OpenAIContent {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> OpenAIContent.Text(element.content)
            is JsonArray -> {
                val parts = jsonDecoder.json.decodeFromJsonElement(
                    ListSerializer(OpenAIContentPartSerializer),
                    element
                )
                OpenAIContent.Parts(parts)
            }
            else -> throw IllegalArgumentException("Unexpected OpenAI content format")
        }
    }
}

/**
 * OpenAI ContentPart 序列化器
 */
object OpenAIContentPartSerializer : KSerializer<OpenAIContentPart> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("OpenAIContentPart")

    override fun serialize(encoder: Encoder, value: OpenAIContentPart) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is OpenAIContentPart.Text -> jsonEncoder.encodeSerializableValue(
                OpenAIContentPart.Text.serializer(), value
            )
            is OpenAIContentPart.ImageUrl -> jsonEncoder.encodeSerializableValue(
                OpenAIContentPart.ImageUrl.serializer(), value
            )
        }
    }

    override fun deserialize(decoder: Decoder): OpenAIContentPart {
        val jsonDecoder = decoder as JsonDecoder
        val json = jsonDecoder.json
        val element = jsonDecoder.decodeJsonElement()
        val type = element.jsonObject["type"]?.let {
            (it as? JsonPrimitive)?.content
        } ?: "text"

        return when (type) {
            "text" -> json.decodeFromJsonElement(OpenAIContentPart.Text.serializer(), element)
            "image_url" -> json.decodeFromJsonElement(OpenAIContentPart.ImageUrl.serializer(), element)
            else -> throw IllegalArgumentException("Unknown content part type: $type")
        }
    }
}

private val kotlinx.serialization.json.JsonElement.jsonObject
    get() = this as kotlinx.serialization.json.JsonObject
