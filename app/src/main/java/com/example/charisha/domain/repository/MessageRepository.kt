package com.example.charisha.domain.repository

import com.example.charisha.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * 消息仓库接口
 */
interface MessageRepository {
    fun observeMessagesByConversation(conversationId: String): Flow<List<Message>>
    fun observeMessagesUpTo(conversationId: String, untilMessageId: String): Flow<List<Message>>
    suspend fun addMessage(message: Message): Result<Unit>
    suspend fun updateMessage(message: Message): Result<Unit>
    suspend fun deleteMessage(id: String): Result<Unit>
    suspend fun deleteMessagesAfter(conversationId: String, afterTimestamp: Long): Result<Unit>
}
