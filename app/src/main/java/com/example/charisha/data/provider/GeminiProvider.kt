package com.example.charisha.data.provider

import com.example.charisha.data.remote.api.GeminiApi
import com.example.charisha.data.remote.dto.gemini.GeminiBlob
import com.example.charisha.data.remote.dto.gemini.GeminiContent
import com.example.charisha.data.remote.dto.gemini.GeminiGenerationConfig
import com.example.charisha.data.remote.dto.gemini.GeminiImageInstance
import com.example.charisha.data.remote.dto.gemini.GeminiImageRequest
import com.example.charisha.data.remote.dto.gemini.GeminiPart
import com.example.charisha.data.remote.dto.gemini.GeminiRequest
import com.example.charisha.data.remote.dto.gemini.GeminiSystemInstruction
import com.example.charisha.data.remote.dto.gemini.GeminiTextPart
import com.example.charisha.data.remote.sse.SSEParser
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.StreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GeminiProvider(
    private val api: GeminiApi,
    private val sseParser: SSEParser,
    private val binaryResolver: BinaryResolver
) : LLMProvider {

    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsVision = true,
        supportsPdf = true,
        supportsFileInlineData = true,
        supportsStreaming = true,
        supportsStreamingSseAlt = true,
        supportsImageGeneration = true
    )

    override fun sendMessage(params: LLMProvider.SendParams): Flow<StreamEvent> = flow {
        val contents = buildContents(params.history, params.newContent)
        val systemInstruction = params.systemPrompt?.takeIf { it.isNotBlank() }?.let {
            GeminiSystemInstruction(parts = listOf(GeminiTextPart(it)))
        }
        val request = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(
                maxOutputTokens = params.maxTokens,
                temperature = params.temperature
            )
        )

        if (params.useStream) {
            val ndjson = api.streamGenerateContent(params.modelId, params.apiKey, request)
            if (ndjson.isSuccessful) {
                ndjson.body()?.let { body ->
                    sseParser.parseGeminiNDJSONStream(body).collect { emit(it) }
                } ?: emit(StreamEvent.Error("HTTP_ERROR", "空响应"))
                return@flow
            }

            // 兼容：fallback 到 SSE 模式 (?alt=sse)
            val sse = api.streamGenerateContentSSE(params.modelId, params.apiKey, alt = "sse", request = request)
            if (!sse.isSuccessful) {
                emit(StreamEvent.Error("HTTP_ERROR", "HTTP ${sse.code()}: ${sse.message()}"))
                return@flow
            }
            sse.body()?.let { body ->
                sseParser.parseGeminiSSEStream(body).collect { emit(it) }
            } ?: emit(StreamEvent.Error("HTTP_ERROR", "空响应"))
        } else {
            val response = api.generateContent(params.modelId, params.apiKey, request)
            if (!response.isSuccessful) {
                emit(StreamEvent.Error("HTTP_ERROR", "HTTP ${response.code()}: ${response.message()}"))
                return@flow
            }
            val body = response.body()
            body?.error?.let { emit(StreamEvent.Error(it.status ?: "ERROR", it.message ?: "Unknown error")) }
            body?.candidates?.firstOrNull()?.content?.parts?.forEach { part ->
                part.thought?.let { emit(StreamEvent.ThinkingDelta(it)) }
                part.text?.let { emit(StreamEvent.TextDelta(it)) }
            }
            emit(StreamEvent.Done)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generateImage(params: LLMProvider.ImageGenParams): Result<StreamEvent.ImageGenerated> = runCatching {
        val response = api.generateImage(
            model = params.modelId,
            apiKey = params.apiKey,
            request = GeminiImageRequest(
                instances = listOf(GeminiImageInstance(prompt = params.prompt))
            )
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code()}: ${response.message()}")
        }
        val pred = response.body()?.predictions?.firstOrNull()
            ?: throw IllegalStateException("空响应")
        val b64 = pred.bytesBase64Encoded ?: throw IllegalStateException("缺少 bytesBase64Encoded")
        val mime = pred.mimeType ?: "image/png"
        StreamEvent.ImageGenerated(base64 = b64, mimeType = mime)
    }

    private fun buildContents(history: List<Message>, newContent: List<ContentPart>): List<GeminiContent> {
        val contents = mutableListOf<GeminiContent>()

        history.filter { it.role != MessageRole.SYSTEM }.forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "model"
                MessageRole.SYSTEM -> return@forEach
            }
            val parts = msg.contentToGeminiParts()
            if (parts.isNotEmpty()) contents.add(GeminiContent(role = role, parts = parts))
        }

        if (newContent.isNotEmpty()) {
            val parts = newContent.contentToGeminiParts()
            if (parts.isNotEmpty()) contents.add(GeminiContent(role = "user", parts = parts))
        }

        return contents
    }

    private fun List<ContentPart>.contentToGeminiParts(): List<GeminiPart> {
        val parts = mutableListOf<GeminiPart>()
        forEach { part ->
            when (part) {
                is ContentPart.Text -> {
                    if (part.text.isNotBlank()) parts.add(GeminiPart.Text(part.text))
                }
                is ContentPart.Image -> {
                    val resolved = binaryResolver.resolveImage(part)
                    parts.add(GeminiPart.InlineData(GeminiBlob(mimeType = resolved.mimeType, data = resolved.base64)))
                }
                is ContentPart.File -> {
                    val extracted = part.extractedText?.takeIf { it.isNotBlank() }
                    if (extracted != null && (part.mimeType.startsWith("text/") || part.mimeType == "application/json")) {
                        parts.add(GeminiPart.Text("文件：${part.fileName}\n\n$extracted"))
                    } else {
                        val resolved = binaryResolver.resolveFile(part)
                        parts.add(GeminiPart.InlineData(GeminiBlob(mimeType = resolved.mimeType, data = resolved.base64)))
                    }
                }
            }
        }
        return parts
    }
}

