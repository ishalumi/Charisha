package com.example.charisha.data.provider

import com.example.charisha.data.remote.api.OpenAIApi
import com.example.charisha.data.remote.dto.openai.ImageUrlData
import com.example.charisha.data.remote.dto.openai.OpenAIChatRequest
import com.example.charisha.data.remote.dto.openai.OpenAIContent
import com.example.charisha.data.remote.dto.openai.OpenAIContentPart
import com.example.charisha.data.remote.dto.openai.OpenAIImageRequest
import com.example.charisha.data.remote.sse.SSEParser
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.StreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class OpenAIProvider(
    private val api: OpenAIApi,
    private val sseParser: SSEParser,
    private val binaryResolver: BinaryResolver
) : LLMProvider {

    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsVision = true,
        supportsPdf = false,
        supportsFileInlineData = false,
        supportsStreaming = true,
        supportsImageGeneration = true
    )

    override fun sendMessage(params: LLMProvider.SendParams): Flow<StreamEvent> = flow {
        val messages = buildMessages(params.systemPrompt, params.history, params.newContent)
        val request = OpenAIChatRequest(
            model = params.modelId,
            messages = messages,
            stream = params.useStream,
            maxTokens = params.maxTokens,
            temperature = params.temperature
        )

        if (params.useStream) {
            val response = api.chatCompletionsStream(OpenAIApi.buildAuthHeader(params.apiKey), request)
            if (!response.isSuccessful) {
                emit(StreamEvent.Error("HTTP_ERROR", "HTTP ${response.code()}: ${response.message()}"))
                return@flow
            }
            response.body()?.let { body ->
                sseParser.parseOpenAIStream(body).collect { emit(it) }
            } ?: emit(StreamEvent.Error("HTTP_ERROR", "空响应"))
        } else {
            val response = api.chatCompletions(OpenAIApi.buildAuthHeader(params.apiKey), request)
            if (!response.isSuccessful) {
                emit(StreamEvent.Error("HTTP_ERROR", "HTTP ${response.code()}: ${response.message()}"))
                return@flow
            }
            response.body()?.choices?.firstOrNull()?.message?.let { msg ->
                msg.reasoningContent?.let { emit(StreamEvent.ThinkingDelta(it)) }
                msg.content?.let { emit(StreamEvent.TextDelta(it)) }
            }
            emit(StreamEvent.Done)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generateImage(params: LLMProvider.ImageGenParams): Result<StreamEvent.ImageGenerated> = runCatching {
        val response = api.generateImage(
            authorization = OpenAIApi.buildAuthHeader(params.apiKey),
            request = OpenAIImageRequest(
                model = params.modelId,
                prompt = params.prompt,
                n = params.n,
                size = params.size,
                responseFormat = "b64_json"
            )
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code()}: ${response.message()}")
        }
        val data = response.body()?.data?.firstOrNull()
            ?: throw IllegalStateException("空响应")
        val base64 = data.b64Json ?: throw IllegalStateException("缺少 b64_json")
        StreamEvent.ImageGenerated(base64 = base64, mimeType = "image/png")
    }

    private fun buildMessages(
        systemPrompt: String?,
        history: List<Message>,
        newContent: List<ContentPart>
    ): List<com.example.charisha.data.remote.dto.openai.OpenAIMessage> {
        val messages = mutableListOf<com.example.charisha.data.remote.dto.openai.OpenAIMessage>()

        systemPrompt?.takeIf { it.isNotBlank() }?.let {
            messages.add(
                com.example.charisha.data.remote.dto.openai.OpenAIMessage(
                    role = "system",
                    content = OpenAIContent.Text(it)
                )
            )
        }

        history.forEach { msg ->
            messages.add(
                com.example.charisha.data.remote.dto.openai.OpenAIMessage(
                    role = roleToOpenAI(msg.role),
                    content = buildContent(msg.content)
                )
            )
        }

        if (newContent.isNotEmpty()) {
            messages.add(
                com.example.charisha.data.remote.dto.openai.OpenAIMessage(
                    role = "user",
                    content = buildContent(newContent)
                )
            )
        }

        return messages
    }

    private fun roleToOpenAI(role: MessageRole): String = when (role) {
        MessageRole.USER -> "user"
        MessageRole.ASSISTANT -> "assistant"
        MessageRole.SYSTEM -> "system"
    }

    private fun buildContent(parts: List<ContentPart>): OpenAIContent {
        val contentParts = mutableListOf<OpenAIContentPart>()
        parts.forEach { part ->
            when (part) {
                is ContentPart.Text -> {
                    if (part.text.isNotBlank()) {
                        contentParts.add(OpenAIContentPart.Text(text = part.text))
                    }
                }
                is ContentPart.Image -> {
                    val url = resolveImageUrl(part)
                    contentParts.add(OpenAIContentPart.ImageUrl(imageUrl = ImageUrlData(url = url)))
                }
                is ContentPart.File -> {
                    // Chat Completions 不支持原始文件输入：仅在已抽取文本时作为 text 发送
                    val extracted = part.extractedText?.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("OpenAI 暂不支持文件输入: ${part.fileName}")
                    contentParts.add(OpenAIContentPart.Text(text = "文件：${part.fileName}\n\n$extracted"))
                }
            }
        }

        if (contentParts.isEmpty()) {
            return OpenAIContent.Text("")
        }
        if (contentParts.size == 1 && contentParts.first() is OpenAIContentPart.Text) {
            return OpenAIContent.Text((contentParts.first() as OpenAIContentPart.Text).text)
        }
        return OpenAIContent.Parts(contentParts)
    }

    private fun resolveImageUrl(image: ContentPart.Image): String {
        val sourceUrl = image.sourceUrl?.takeIf { it.isNotBlank() }
        if (sourceUrl != null) return sourceUrl

        val resolved = binaryResolver.resolveImage(image)
        return "data:${resolved.mimeType};base64,${resolved.base64}"
    }
}

