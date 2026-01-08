package com.example.charisha.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * LLM 模型配置实体
 */
@Entity(
    tableName = "models",
    primaryKeys = ["id", "channelId"],
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("channelId")]
)
data class ModelEntity(
    val id: String,
    val channelId: String,
    val displayName: String,
    val contextLength: Int,
    val maxOutputTokens: Int,
    val supportsVision: Boolean,
    val supportsImageGen: Boolean,
    val supportsReasoning: Boolean,
    val supportsStreaming: Boolean,
    val defaultTemperature: Float,
    val defaultTopP: Float,
    val isEnabled: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)
