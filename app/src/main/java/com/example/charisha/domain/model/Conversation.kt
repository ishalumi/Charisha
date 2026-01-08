package com.example.charisha.domain.model

/**
 * 对话会话
 */
data class Conversation(
    val id: String,
    val channelId: String,
    val modelId: String? = null,
    val title: String,
    val systemPrompt: String? = null,
    val parentMessageId: String? = null,
    val rootConversationId: String? = null,
    val lastMessageTime: Long,
    val createdAt: Long
) {
    val isBranch: Boolean
        get() = parentMessageId != null
}
