package com.example.charisha.data.provider

import com.example.charisha.data.remote.api.ClaudeApi
import com.example.charisha.data.remote.dto.claude.ClaudeContentBlock
import com.example.charisha.data.remote.dto.claude.ClaudeDocumentSource
import com.example.charisha.data.remote.dto.claude.ClaudeImageSource
import com.example.charisha.data.remote.dto.claude.ClaudeMessage
import com.example.charisha.data.remote.dto.claude.ClaudeRequest
import com.example.charisha.data.remote.sse.SSEParser
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.StreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ClaudeProvider(
    private val api: ClaudeApi,
    private val sseParser: SSEParser,
    private val binaryResolver: BinaryResolver
) : LLMProvider {

    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsVision = true,
        supportsPdf = true,
        supportsFileInlineData = false,
        supportsStreaming = true,
        supportsImageGeneration = false
    )

    override fun sendMessage(params: LLMProvider.SendParams): Flow<StreamEvent> = flow {
        val messages = buildMessages(params.history, params.newContent)
        val request = ClaudeRequest(
            model = params.modelId,
            messages = messages,
            system = params.systemPrompt,
            maxTokens = params.maxTokens,
            stream = params.useStream,
            temperature = params.temperature
        )

        if (params.useStream) {
            val response = api.createMessageStream(apiKey = params.apiKey, request = request)
            if (!response.isSuccessful) {
                emit(StreamEvent.Error("HTTP_ERROR", "HTTP ${response.code()}: ${response.message()}"))
                return@flow
            }
            response.body()?.let { body ->
                sseParser.parseClaudeStream(body).collect { emit(it) }
            } ?: emit(StreamEvent.Error("HTTP_ERROR", "空响应"))
        } else {
            val response = api.createMessage(apiKey = params.apiKey, request = request)
            if (!response.isSuccessful) {
                emit(StreamEvent.Error("HTTP_ERROR", "HTTP ${response.code()}: ${response.message()}"))
                return@flow
            }
            response.body()?.content?.forEach { block ->
                block.thinking?.let { emit(StreamEvent.ThinkingDelta(it)) }
                block.text?.let { emit(StreamEvent.TextDelta(it)) }
            }
            emit(StreamEvent.Done)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generateImage(params: LLMProvider.ImageGenParams): Result<StreamEvent.ImageGenerated> {
        return Result.failure(UnsupportedOperationException("Claude 暂不支持图片生成"))
    }

    private fun buildMessages(history: List<Message>, newContent: List<ContentPart>): List<ClaudeMessage> {
        val messages = mutableListOf<ClaudeMessage>()

        history.filter { it.role != MessageRole.SYSTEM }.forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM, MessageRole.UNKNOWN -> return@forEach
            }
            val blocks = msg.content.toClaudeBlocks()
            if (blocks.isNotEmpty()) messages.add(ClaudeMessage(role = role, content = blocks))
        }

        if (newContent.isNotEmpty()) {
            val blocks = newContent.toClaudeBlocks()
            if (blocks.isNotEmpty()) messages.add(ClaudeMessage(role = "user", content = blocks))
        }

        return messages
    }

    private fun List<ContentPart>.toClaudeBlocks(): List<ClaudeContentBlock> {
        val blocks = mutableListOf<ClaudeContentBlock>()
        forEach { part ->
            when (part) {
                is ContentPart.Text -> {
                    if (part.text.isNotBlank()) blocks.add(ClaudeContentBlock.Text(text = part.text))
                }
                is ContentPart.Image -> {
                    val resolved = binaryResolver.resolveImage(part)
                    blocks.add(
                        ClaudeContentBlock.Image(
                            source = ClaudeImageSource(
                                mediaType = resolved.mimeType,
                                data = resolved.base64
                            )
                        )
                    )
                }
                is ContentPart.File -> {
                    if (part.mimeType == "application/pdf") {
                        val resolved = binaryResolver.resolveFile(part)
                        blocks.add(
                            ClaudeContentBlock.Document(
                                source = ClaudeDocumentSource(
                                    mediaType = resolved.mimeType,
                                    data = resolved.base64
                                )
                            )
                        )
                    } else {
                        val extracted = part.extractedText?.takeIf { it.isNotBlank() }
                            ?: throw IllegalArgumentException("Claude 暂不支持该文件类型: ${part.fileName}")
                        blocks.add(ClaudeContentBlock.Text(text = "文件：${part.fileName}\n\n$extracted"))
                    }
                }
            }
        }
        return blocks
    }
}

