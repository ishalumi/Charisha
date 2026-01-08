package com.example.charisha.data.remote.dto.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Gemini generateContent 请求
 */
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiSystemInstruction(
    val parts: List<GeminiTextPart>
)

@Serializable
data class GeminiTextPart(
    val text: String
)

/**
 * Gemini 内容块
 */
@Serializable
sealed interface GeminiPart {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : GeminiPart

    @Serializable
    @SerialName("inline_data")
    data class InlineData(
        @SerialName("inline_data")
        val inlineData: GeminiBlob
    ) : GeminiPart

    @Serializable
    @SerialName("file_data")
    data class FileData(
        @SerialName("file_data")
        val fileData: GeminiFileData
    ) : GeminiPart
}

@Serializable
data class GeminiBlob(
    @SerialName("mime_type")
    val mimeType: String,
    val data: String
)

@Serializable
data class GeminiFileData(
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_uri")
    val fileUri: String
)

@Serializable
data class GeminiGenerationConfig(
    val maxOutputTokens: Int? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

/**
 * Gemini generateContent 响应
 */
@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val usageMetadata: GeminiUsageMetadata? = null,
    val error: GeminiError? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiResponseContent? = null,
    val finishReason: String? = null,
    val index: Int? = null
)

@Serializable
data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>? = null,
    val role: String? = null
)

@Serializable
data class GeminiResponsePart(
    val text: String? = null,
    val thought: String? = null,
    val inlineData: GeminiBlob? = null
)

@Serializable
data class GeminiUsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
)

/**
 * Gemini 模型列表响应 (原生 API)
 */
@Serializable
data class GeminiModelsResponse(
    val models: List<GeminiModelInfo>? = null
)

@Serializable
data class GeminiModelInfo(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val inputTokenLimit: Int? = null,
    val outputTokenLimit: Int? = null,
    val supportedGenerationMethods: List<String>? = null
)

/**
 * Gemini Imagen 图片生成请求
 */
@Serializable
data class GeminiImageRequest(
    val instances: List<GeminiImageInstance>,
    val parameters: GeminiImageParameters? = null
)

@Serializable
data class GeminiImageInstance(
    val prompt: String
)

@Serializable
data class GeminiImageParameters(
    val sampleCount: Int = 1,
    val aspectRatio: String? = null
)

/**
 * Gemini Imagen 图片生成响应
 */
@Serializable
data class GeminiImageResponse(
    val predictions: List<GeminiImagePrediction>? = null
)

@Serializable
data class GeminiImagePrediction(
    val bytesBase64Encoded: String? = null,
    val mimeType: String? = null
)

/**
 * Gemini 错误
 */
@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
data class GeminiErrorResponse(
    val error: GeminiError
)
