package com.example.charisha.data.remote.dto.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI Chat Completions 请求
 */
@Serializable
data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val stream: Boolean = false,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val temperature: Float? = null,
    @SerialName("top_p")
    val topP: Float? = null
)

/**
 * OpenAI 消息
 */
@Serializable
data class OpenAIMessage(
    val role: String,
    val content: OpenAIContent
)

/**
 * OpenAI 消息内容 - 支持文本和多模态
 */
@Serializable(with = OpenAIContentSerializer::class)
sealed interface OpenAIContent {
    @Serializable
    @JvmInline
    value class Text(val text: String) : OpenAIContent

    @Serializable
    @JvmInline
    value class Parts(val parts: List<OpenAIContentPart>) : OpenAIContent
}

/**
 * OpenAI 内容块
 */
@Serializable
sealed interface OpenAIContentPart {
    @Serializable
    @SerialName("text")
    data class Text(
        val type: String = "text",
        val text: String
    ) : OpenAIContentPart

    @Serializable
    @SerialName("image_url")
    data class ImageUrl(
        val type: String = "image_url",
        @SerialName("image_url")
        val imageUrl: ImageUrlData
    ) : OpenAIContentPart
}

@Serializable
data class ImageUrlData(
    val url: String,
    val detail: String? = null
)

/**
 * OpenAI Chat Completions 响应 (非流式)
 */
@Serializable
data class OpenAIChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null
)

@Serializable
data class OpenAIChoice(
    val index: Int,
    val message: OpenAIResponseMessage? = null,
    val delta: OpenAIDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAIResponseMessage(
    val role: String,
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null
)

@Serializable
data class OpenAIDelta(
    val role: String? = null,
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null
)

@Serializable
data class OpenAIUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

/**
 * OpenAI 模型列表响应
 */
@Serializable
data class OpenAIModelsResponse(
    val `object`: String,
    val data: List<OpenAIModelInfo>
)

@Serializable
data class OpenAIModelInfo(
    val id: String,
    val `object`: String? = null,
    val created: Long? = null,
    @SerialName("owned_by")
    val ownedBy: String? = null
)

/**
 * OpenAI 图片生成请求
 */
@Serializable
data class OpenAIImageRequest(
    val model: String,
    val prompt: String,
    val n: Int = 1,
    val size: String = "1024x1024",
    @SerialName("response_format")
    val responseFormat: String = "b64_json"
)

/**
 * OpenAI 图片生成响应
 */
@Serializable
data class OpenAIImageResponse(
    val created: Long,
    val data: List<OpenAIImageData>
)

@Serializable
data class OpenAIImageData(
    @SerialName("b64_json")
    val b64Json: String? = null,
    val url: String? = null,
    @SerialName("revised_prompt")
    val revisedPrompt: String? = null
)

/**
 * OpenAI 错误响应
 */
@Serializable
data class OpenAIErrorResponse(
    val error: OpenAIError
)

@Serializable
data class OpenAIError(
    val message: String,
    val type: String? = null,
    val param: String? = null,
    val code: String? = null
)
