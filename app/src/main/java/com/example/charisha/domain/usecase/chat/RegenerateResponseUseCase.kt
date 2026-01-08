package com.example.charisha.domain.usecase.chat

import com.example.charisha.domain.model.StreamEvent
import com.example.charisha.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 重新生成响应用例
 */
class RegenerateResponseUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(messageId: String): Flow<StreamEvent> {
        return chatRepository.regenerateResponse(messageId)
    }
}
