package com.example.charisha.presentation.navigation

object Routes {
    const val CHAT = "chat?conversationId={conversationId}"
    const val CHANNEL_LIST = "channel_list"
    const val CHANNEL_EDIT = "channel_edit/{channelId}"
    const val CONVERSATION_LIST = "conversation_list"

    fun chat(conversationId: String? = null): String {
        return if (conversationId.isNullOrBlank()) {
            "chat"
        } else {
            "chat?conversationId=$conversationId"
        }
    }

    fun channelEdit(channelId: String? = null) = "channel_edit/${channelId ?: "new"}"
}
