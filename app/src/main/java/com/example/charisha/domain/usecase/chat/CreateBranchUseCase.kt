package com.example.charisha.domain.usecase.chat

import com.example.charisha.domain.model.Conversation
import com.example.charisha.domain.repository.ConversationRepository
import javax.inject.Inject

/**
 * 创建对话分支用例
 */
class CreateBranchUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(fromMessageId: String): Result<Conversation> {
        return conversationRepository.createBranch(fromMessageId)
    }
}
