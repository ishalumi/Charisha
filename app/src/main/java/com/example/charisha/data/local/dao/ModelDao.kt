package com.example.charisha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.charisha.data.local.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

/**
 * 模型 DAO
 */
@Dao
interface ModelDao {

    @Query("SELECT * FROM models WHERE channelId = :channelId ORDER BY sortOrder ASC")
    fun observeByChannel(channelId: String): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE channelId = :channelId AND isEnabled = 1 ORDER BY sortOrder ASC")
    fun observeEnabledByChannel(channelId: String): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE id = :id AND channelId = :channelId")
    fun observeById(id: String, channelId: String): Flow<ModelEntity?>

    @Query("SELECT * FROM models WHERE id = :id AND channelId = :channelId")
    suspend fun getById(id: String, channelId: String): ModelEntity?

    @Upsert
    suspend fun upsertAll(models: List<ModelEntity>)

    @Update
    suspend fun update(model: ModelEntity)

    @Query("DELETE FROM models WHERE channelId = :channelId")
    suspend fun deleteByChannel(channelId: String)

    @Transaction
    suspend fun replaceModelsForChannel(channelId: String, models: List<ModelEntity>) {
        deleteByChannel(channelId)
        upsertAll(models)
    }
}
