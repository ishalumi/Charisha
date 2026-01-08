package com.example.charisha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.charisha.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

/**
 * 渠道 DAO
 */
@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    fun observeById(id: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getById(id: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(channel: ChannelEntity)

    @Upsert
    suspend fun upsert(channel: ChannelEntity)

    @Update
    suspend fun update(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun count(): Int
}
