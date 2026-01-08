package com.example.charisha.data.remote.dto.claude

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Claude Messages 请求
 */
@Serializable
data class ClaudeRequest(
    val model: String,
    val messages: List<ClaudeMessage>,
    val system: String? = null,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val stream: Boolean = false,
    val temperature: Float? = null,
    @SerialName("top_p")
    val topP: Float? = null
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: List<ClaudeContentBlock>
)

/**
 * Claude 内容块
 */
@Serializable
sealed interface ClaudeContentBlock {
    @Serializable
    @SerialName("text")
    data class Text(
        val type: String = "text",
        val text: String
    ) : ClaudeContentBlock

    @Serializable
    @SerialName("image")
    data class Image(
        val type: String = "image",
        val source: ClaudeImageSource
    ) : ClaudeContentBlock

    @Serializable
    @SerialName("document")
    data class Document(
        val type: String = "document",
        val source: ClaudeDocumentSource
    ) : ClaudeContentBlock
}

@Serializable
data class ClaudeImageSource(
    val type: String = "base64",
    @SerialName("media_type")
    val mediaType: String,
    val data: String
)

@Serializable
data class ClaudeDocumentSource(
    val type: String = "base64",
    @SerialName("media_type")
    val mediaType: String,
    val data: String
)

/**
 * Claude Messages 响应 (非流式)
 */
@Serializable
data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeResponseContent>,
    val model: String,
    @SerialName("stop_reason")
    val stopReason: String? = null,
    @SerialName("stop_sequence")
    val stopSequence: String? = null,
    val usage: ClaudeUsage? = null
)

@Serializable
data class ClaudeResponseContent(
    val type: String,
    val text: String? = null,
    val thinking: String? = null
)

@Serializable
data class ClaudeUsage(
    @SerialName("input_tokens")
    val inputTokens: Int,
    @SerialName("output_tokens")
    val outputTokens: Int
)

/**
 * Claude 流式事件
 */
@Serializable
data class ClaudeStreamEvent(
    val type: String,
    val message: ClaudeStreamMessage? = null,
    val index: Int? = null,
    @SerialName("content_block")
    val contentBlock: ClaudeStreamContentBlock? = null,
    val delta: ClaudeStreamDelta? = null,
    val usage: ClaudeUsage? = null,
    val error: ClaudeError? = null
)

@Serializable
data class ClaudeStreamMessage(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val model: String? = null,
    val usage: ClaudeUsage? = null
)

@Serializable
data class ClaudeStreamContentBlock(
    val type: String,
    val text: String? = null
)

@Serializable
data class ClaudeStreamDelta(
    val type: String? = null,
    val text: String? = null,
    val thinking: String? = null,
    @SerialName("stop_reason")
    val stopReason: String? = null
)

/**
 * Claude 模型列表响应
 */
@Serializable
data class ClaudeModelsResponse(
    val data: List<ClaudeModelInfo>,
    @SerialName("has_more")
    val hasMore: Boolean,
    @SerialName("first_id")
    val firstId: String? = null,
    @SerialName("last_id")
    val lastId: String? = null
)

@Serializable
data class ClaudeModelInfo(
    val id: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val type: String? = null
)

/**
 * Claude 错误响应
 */
@Serializable
data class ClaudeErrorResponse(
    val type: String,
    val error: ClaudeError
)

@Serializable
data class ClaudeError(
    val type: String,
    val message: String
)
