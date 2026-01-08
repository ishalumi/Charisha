package com.example.charisha.domain.repository

import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.StreamEvent
import kotlinx.coroutines.flow.Flow

/**
 * 聊天仓库接口
 */
interface ChatRepository {
    fun sendMessage(
        conversationId: String,
        content: List<ContentPart>,
        streamEnabled: Boolean
    ): Flow<StreamEvent>

    fun regenerateResponse(messageId: String): Flow<StreamEvent>

    suspend fun generateImage(
        conversationId: String,
        prompt: String,
        size: String = "1024x1024",
        n: Int = 1
    ): Result<StreamEvent.ImageGenerated>

    suspend fun cancelGeneration(conversationId: String)
}
