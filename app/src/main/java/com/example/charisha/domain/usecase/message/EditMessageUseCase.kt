package com.example.charisha.domain.usecase.message

import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.StreamEvent
import com.example.charisha.domain.repository.ChatRepository
import com.example.charisha.domain.repository.ConversationRepository
import com.example.charisha.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

/**
 * 编辑消息用例
 */
class EditMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val chatRepository: ChatRepository
) {
    /**
     * 编辑消息
     * - 用户消息：编辑后删除后续消息并重新生成 AI 响应
     * - AI 消息：直接修改内容，不重新生成
     */
    operator fun invoke(params: Params): Flow<StreamEvent> = flow {
        val messages = messageRepository.observeMessagesByConversation(params.conversationId).first()
        val targetMessage = messages.find { it.id == params.messageId }
            ?: throw IllegalArgumentException("消息不存在: ${params.messageId}")

        val now = System.currentTimeMillis()

        when (targetMessage.role) {
            MessageRole.USER -> {
                val updatedMessage = targetMessage.copy(
                    content = params.newContent,
                    isEdited = true,
                    editedAt = now
                )
                messageRepository.updateMessage(updatedMessage)

                messageRepository.deleteMessagesAfter(params.conversationId, targetMessage.createdAt)

                val textBuilder = StringBuilder()
                val thinkingBuilder = StringBuilder()

                chatRepository.sendMessage(
                    conversationId = params.conversationId,
                    content = params.newContent,
                    streamEnabled = params.streamEnabled
                ).collect { event ->
                    when (event) {
                        is StreamEvent.TextDelta -> textBuilder.append(event.text)
                        is StreamEvent.ThinkingDelta -> thinkingBuilder.append(event.text)
                        else -> {}
                    }
                    emit(event)
                }

                val assistantMessage = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = params.conversationId,
                    parentId = params.messageId,
                    role = MessageRole.ASSISTANT,
                    content = listOf(ContentPart.Text(textBuilder.toString())),
                    thinking = thinkingBuilder.toString().takeIf { it.isNotEmpty() },
                    thinkingCollapsed = true,
                    modelUsed = null,
                    tokenCount = null,
                    isEdited = false,
                    editedAt = null,
                    createdAt = System.currentTimeMillis()
                )
                messageRepository.addMessage(assistantMessage)

                val conversation = conversationRepository.observeConversationById(params.conversationId)
                    .filterNotNull()
                    .first()
                conversationRepository.updateConversation(
                    conversation.copy(lastMessageTime = assistantMessage.createdAt)
                )
            }

            MessageRole.ASSISTANT -> {
                val updatedMessage = targetMessage.copy(
                    content = params.newContent,
                    isEdited = true,
                    editedAt = now
                )
                messageRepository.updateMessage(updatedMessage)
                emit(StreamEvent.Done)
            }

            MessageRole.SYSTEM -> {
                throw IllegalArgumentException("系统消息不可编辑")
            }

            MessageRole.UNKNOWN -> {
                throw IllegalArgumentException("未知消息类型不可编辑")
            }
        }
    }

    data class Params(
        val conversationId: String,
        val messageId: String,
        val newContent: List<ContentPart>,
        val streamEnabled: Boolean = true
    )
}
