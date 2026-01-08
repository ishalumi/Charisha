package com.example.charisha.domain.usecase.chat

import com.example.charisha.data.attachments.AttachmentRepository
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.repository.ChatRepository
import com.example.charisha.domain.repository.ConversationRepository
import com.example.charisha.domain.repository.MessageRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * 图片生成用例
 */
class GenerateImageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val attachmentRepository: AttachmentRepository,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(params: Params): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val prompt = params.prompt.trim()
        require(prompt.isNotBlank()) { "提示词不能为空" }

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            conversationId = params.conversationId,
            parentId = null,
            role = MessageRole.USER,
            content = listOf(ContentPart.Text("生成图片：$prompt")),
            thinking = null,
            thinkingCollapsed = true,
            modelUsed = null,
            tokenCount = null,
            isEdited = false,
            editedAt = null,
            createdAt = now
        )
        messageRepository.addMessage(userMessage).getOrThrow()

        val imageEvent = chatRepository.generateImage(
            conversationId = params.conversationId,
            prompt = prompt,
            size = params.size,
            n = params.n
        ).getOrThrow()

        val imagePart = attachmentRepository.saveGeneratedImage(
            base64 = imageEvent.base64,
            mimeType = imageEvent.mimeType
        ).getOrThrow()

        val assistantMessage = Message(
            id = UUID.randomUUID().toString(),
            conversationId = params.conversationId,
            parentId = userMessage.id,
            role = MessageRole.ASSISTANT,
            content = listOf(imagePart),
            thinking = null,
            thinkingCollapsed = true,
            modelUsed = null,
            tokenCount = null,
            isEdited = false,
            editedAt = null,
            createdAt = System.currentTimeMillis()
        )
        messageRepository.addMessage(assistantMessage).getOrThrow()

        val conversation = conversationRepository.observeConversationById(params.conversationId)
            .filterNotNull()
            .first()
        conversationRepository.updateConversation(
            conversation.copy(lastMessageTime = assistantMessage.createdAt)
        ).getOrThrow()
    }

    data class Params(
        val conversationId: String,
        val prompt: String,
        val size: String = "1024x1024",
        val n: Int = 1
    )
}

