package com.example.charisha.data.repository

import com.example.charisha.data.local.dao.MessageDao
import com.example.charisha.data.local.mapper.EntityMapper.toDomain
import com.example.charisha.data.local.mapper.EntityMapper.toEntity
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val json: Json
) : MessageRepository {

    override fun observeMessagesByConversation(conversationId: String): Flow<List<Message>> =
        messageDao.observeByConversation(conversationId).map { entities ->
            entities.map { entity ->
                val content = json.decodeFromString<List<ContentPart>>(entity.contentJson)
                entity.toDomain(content)
            }
        }

    override fun observeMessagesUpTo(
        conversationId: String,
        untilMessageId: String
    ): Flow<List<Message>> =
        messageDao.observeUpTo(conversationId, untilMessageId).map { entities ->
            entities.map { entity ->
                val content = json.decodeFromString<List<ContentPart>>(entity.contentJson)
                entity.toDomain(content)
            }
        }

    override suspend fun addMessage(message: Message): Result<Unit> = runCatching {
        messageDao.insert(message.toEntity(json))
    }

    override suspend fun updateMessage(message: Message): Result<Unit> = runCatching {
        messageDao.update(message.toEntity(json))
    }

    override suspend fun deleteMessage(id: String): Result<Unit> = runCatching {
        messageDao.deleteById(id)
    }

    override suspend fun deleteMessagesAfter(
        conversationId: String,
        afterTimestamp: Long
    ): Result<Unit> = runCatching {
        messageDao.deleteAfter(conversationId, afterTimestamp)
    }
}
