package com.example.charisha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.charisha.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 消息 DAO
 */
@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun observeByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        AND (createdAt < (SELECT createdAt FROM messages WHERE id = :untilMessageId)
             OR (createdAt = (SELECT createdAt FROM messages WHERE id = :untilMessageId) AND id <= :untilMessageId))
        ORDER BY createdAt ASC, id ASC
    """)
    fun observeUpTo(conversationId: String, untilMessageId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        AND (createdAt < (SELECT createdAt FROM messages WHERE id = :untilMessageId)
             OR (createdAt = (SELECT createdAt FROM messages WHERE id = :untilMessageId) AND id <= :untilMessageId))
        ORDER BY createdAt ASC, id ASC
    """)
    suspend fun getUpToSync(conversationId: String, untilMessageId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    suspend fun getByConversationSync(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND createdAt > :afterTimestamp")
    suspend fun deleteAfter(conversationId: String, afterTimestamp: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun countByConversation(conversationId: String): Int
}
