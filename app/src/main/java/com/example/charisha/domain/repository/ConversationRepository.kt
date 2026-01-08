package com.example.charisha.domain.repository

import com.example.charisha.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

/**
 * 对话仓库接口
 */
interface ConversationRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeConversationById(id: String): Flow<Conversation?>
    fun observeConversationsByChannel(channelId: String): Flow<List<Conversation>>
    fun observeBranchesByRoot(rootId: String): Flow<List<Conversation>>
    suspend fun createConversation(conversation: Conversation): Result<Unit>
    suspend fun createBranch(fromMessageId: String): Result<Conversation>
    suspend fun updateConversation(conversation: Conversation): Result<Unit>
    suspend fun deleteConversation(id: String): Result<Unit>
}
