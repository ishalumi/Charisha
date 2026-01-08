package com.example.charisha.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charisha.domain.model.Conversation
import com.example.charisha.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            conversationRepository.observeConversations().collectLatest { conversations ->
                _uiState.value = _uiState.value.copy(conversations = conversations)
            }
        }
    }

    fun createNewConversation(channelId: String) {
        viewModelScope.launch {
            // Placeholder: Create a new conversation
            val now = System.currentTimeMillis()
            val newConversation = Conversation(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                title = "New Chat",
                lastMessageTime = now,
                createdAt = now
            )
            conversationRepository.createConversation(newConversation)
            // Navigate to new conversation handled by UI observing list or a one-time event
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
        }
    }
}

data class ConversationUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
