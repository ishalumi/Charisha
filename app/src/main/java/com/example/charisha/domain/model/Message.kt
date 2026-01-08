package com.example.charisha.domain.model

/**
 * 消息
 */
data class Message(
    val id: String,
    val conversationId: String,
    val parentId: String? = null,
    val role: MessageRole,
    val content: List<ContentPart>,
    val thinking: String? = null,
    val thinkingCollapsed: Boolean = true,
    val modelUsed: String? = null,
    val tokenCount: Int? = null,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val createdAt: Long
) {
    val textContent: String
        get() = content.filterIsInstance<ContentPart.Text>()
            .joinToString("") { it.text }

    val hasThinking: Boolean
        get() = !thinking.isNullOrBlank()

    val canBranch: Boolean
        get() = role == MessageRole.ASSISTANT

    val canEdit: Boolean
        get() = role != MessageRole.SYSTEM
}
