package com.example.charisha.domain.usecase.chat

import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.StreamEvent
import com.example.charisha.domain.repository.ChatRepository
import com.example.charisha.domain.repository.ConversationRepository
import com.example.charisha.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * 发送消息用例
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke(params: Params): Flow<StreamEvent> = flow {
        val now = System.currentTimeMillis()

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            conversationId = params.conversationId,
            parentId = null,
            role = MessageRole.USER,
            content = params.content,
            thinking = null,
            thinkingCollapsed = true,
            modelUsed = null,
            tokenCount = null,
            isEdited = false,
            editedAt = null,
            createdAt = now
        )
        messageRepository.addMessage(userMessage)

        val textBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()

        chatRepository.sendMessage(
            conversationId = params.conversationId,
            content = params.content,
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
            parentId = userMessage.id,
            role = MessageRole.ASSISTANT,
            content = listOf(ContentPart.Text(textBuilder.toString())),
            thinking = thinkingBuilder.toString().takeIf { it.isNotEmpty() },
            thinkingCollapsed = true,
            modelUsed = params.modelId,
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

    data class Params(
        val conversationId: String,
        val content: List<ContentPart>,
        val streamEnabled: Boolean = true,
        val modelId: String? = null
    )
}
