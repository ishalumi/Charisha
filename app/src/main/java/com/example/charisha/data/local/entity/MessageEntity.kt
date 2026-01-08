package com.example.charisha.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 消息实体
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("conversationId"),
        Index("parentId")
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val parentId: String?,
    val role: String,
    val contentJson: String,
    val thinkingJson: String?,
    val thinkingCollapsed: Boolean,
    val modelUsed: String?,
    val tokenCount: Int?,
    val isEdited: Boolean,
    val editedAt: Long?,
    val createdAt: Long
)
