package com.example.charisha.presentation.chat

import com.example.charisha.domain.model.Channel
import com.example.charisha.domain.model.Conversation
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.LlmModel
import com.example.charisha.domain.model.Message

data class ChatUiState(
    // 渠道和模型
    val channels: List<Channel> = emptyList(),
    val currentChannel: Channel? = null,
    val models: List<LlmModel> = emptyList(),
    val currentModel: LlmModel? = null,

    // 对话
    val conversations: List<Conversation> = emptyList(),
    val currentConversation: Conversation? = null,
    val messages: List<Message> = emptyList(),

    // 输入
    val inputText: String = "",
    val pendingAttachments: List<ContentPart> = emptyList(),

    // 流式响应状态
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val streamingThinking: String = "",

    // 错误
    val error: String? = null
)

/**
 * 流式消息状态 - 用于实时显示 AI 响应
 */
data class StreamingMessage(
    val content: String = "",
    val thinking: String = "",
    val isComplete: Boolean = false
)
