package com.example.charisha.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 对话会话实体
 */
@Entity(
    tableName = "conversations",
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
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val channelId: String,
    val modelId: String?,
    val title: String,
    val systemPrompt: String?,
    val parentMessageId: String?,
    val rootConversationId: String?,
    val lastMessageTime: Long,
    val createdAt: Long
)
