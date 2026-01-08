package com.example.charisha.data.repository

import com.example.charisha.data.local.dao.ConversationDao
import com.example.charisha.data.local.dao.MessageDao
import com.example.charisha.data.local.mapper.EntityMapper.toDomain
import com.example.charisha.data.local.mapper.EntityMapper.toEntity
import com.example.charisha.data.local.entity.MessageEntity
import com.example.charisha.domain.model.Conversation
import com.example.charisha.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : ConversationRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeConversationById(id: String): Flow<Conversation?> =
        conversationDao.observeById(id).map { it?.toDomain() }

    override fun observeConversationsByChannel(channelId: String): Flow<List<Conversation>> =
        conversationDao.observeByChannel(channelId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeBranchesByRoot(rootId: String): Flow<List<Conversation>> =
        conversationDao.observeBranchesByRoot(rootId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createConversation(conversation: Conversation): Result<Unit> =
        runCatching {
            conversationDao.insert(conversation.toEntity())
        }

    override suspend fun createBranch(fromMessageId: String): Result<Conversation> = runCatching {
        val sourceMessage = messageDao.getById(fromMessageId)
            ?: throw IllegalArgumentException("消息不存在: $fromMessageId")

        val sourceConversation = conversationDao.getById(sourceMessage.conversationId)
            ?: throw IllegalArgumentException("对话不存在: ${sourceMessage.conversationId}")

        val now = System.currentTimeMillis()
        val rootId = sourceConversation.rootConversationId ?: sourceConversation.id

        val branchConversation = Conversation(
            id = UUID.randomUUID().toString(),
            channelId = sourceConversation.channelId,
            modelId = sourceConversation.modelId,
            title = "${sourceConversation.title} (分支)",
            systemPrompt = sourceConversation.systemPrompt,
            parentMessageId = fromMessageId,
            rootConversationId = rootId,
            lastMessageTime = now,
            createdAt = now
        )

        conversationDao.insert(branchConversation.toEntity())

        // 继承上下文：复制源对话从开头到分支点（包含分支点）之间的消息到新对话中
        val sourceMessages = messageDao.getUpToSync(sourceMessage.conversationId, fromMessageId)
        if (sourceMessages.isNotEmpty()) {
            val idMapping = sourceMessages.associate { it.id to UUID.randomUUID().toString() }
            val copied = sourceMessages.map { entity ->
                MessageEntity(
                    id = idMapping.getValue(entity.id),
                    conversationId = branchConversation.id,
                    parentId = entity.parentId?.let { idMapping[it] },
                    role = entity.role,
                    contentJson = entity.contentJson,
                    thinkingJson = entity.thinkingJson,
                    thinkingCollapsed = entity.thinkingCollapsed,
                    modelUsed = entity.modelUsed,
                    tokenCount = entity.tokenCount,
                    isEdited = entity.isEdited,
                    editedAt = entity.editedAt,
                    createdAt = entity.createdAt
                )
            }
            messageDao.insertAll(copied)
        }

        branchConversation
    }

    override suspend fun updateConversation(conversation: Conversation): Result<Unit> =
        runCatching {
            conversationDao.update(conversation.toEntity())
        }

    override suspend fun deleteConversation(id: String): Result<Unit> = runCatching {
        conversationDao.deleteById(id)
    }
}
