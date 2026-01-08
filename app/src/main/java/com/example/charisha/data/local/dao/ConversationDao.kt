package com.example.charisha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.charisha.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * 对话 DAO
 */
@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE channelId = :channelId ORDER BY lastMessageTime DESC")
    fun observeByChannel(channelId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE rootConversationId = :rootId OR id = :rootId ORDER BY createdAt ASC")
    fun observeBranchesByRoot(rootId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(conversation: ConversationEntity)

    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)
}
