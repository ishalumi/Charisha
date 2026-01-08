package com.example.charisha.data.provider

import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.StreamEvent
import kotlinx.coroutines.flow.Flow

/**
 * 统一 Provider 适配器接口
 */
interface LLMProvider {

    val capabilities: ProviderCapabilities

    fun sendMessage(params: SendParams): Flow<StreamEvent>

    suspend fun generateImage(params: ImageGenParams): Result<StreamEvent.ImageGenerated>

    data class SendParams(
        val modelId: String,
        val apiKey: String,
        val systemPrompt: String?,
        val history: List<Message>,
        val newContent: List<ContentPart>,
        val useStream: Boolean,
        val maxTokens: Int,
        val temperature: Float
    )

    data class ImageGenParams(
        val modelId: String,
        val apiKey: String,
        val prompt: String,
        val size: String = "1024x1024",
        val n: Int = 1
    )
}

