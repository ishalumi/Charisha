package com.example.charisha.presentation.chat

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charisha.data.attachments.AttachmentRepository
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Conversation
import com.example.charisha.domain.model.LlmModel
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.StreamEvent
import com.example.charisha.domain.repository.ChannelRepository
import com.example.charisha.domain.repository.ConversationRepository
import com.example.charisha.domain.repository.MessageRepository
import com.example.charisha.domain.repository.ModelRepository
import com.example.charisha.domain.usecase.chat.RegenerateResponseUseCase
import com.example.charisha.domain.usecase.chat.SendMessageUseCase
import com.example.charisha.domain.usecase.chat.CreateBranchUseCase
import com.example.charisha.domain.usecase.chat.GenerateImageUseCase
import com.example.charisha.domain.usecase.message.EditMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val regenerateResponseUseCase: RegenerateResponseUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val createBranchUseCase: CreateBranchUseCase,
    private val generateImageUseCase: GenerateImageUseCase,
    private val attachmentRepository: AttachmentRepository,
    private val channelRepository: ChannelRepository,
    private val modelRepository: ModelRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var sendMessageJob: Job? = null
    private var conversationObserverJob: Job? = null
    private var messageObserverJob: Job? = null

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            channelRepository.observeChannels().collectLatest { channels ->
                _uiState.update { it.copy(channels = channels) }
                // 自动选择第一个渠道
                if (channels.isNotEmpty() && _uiState.value.currentChannel == null) {
                    selectChannel(channels.first().id)
                }
            }
        }
    }

    fun selectChannel(channelId: String) {
        viewModelScope.launch {
            channelRepository.observeChannelById(channelId).collectLatest { channel ->
                channel?.let {
                    _uiState.update { state -> state.copy(currentChannel = channel) }
                    loadModelsForChannel(channelId)
                }
            }
        }
    }

    private fun loadModelsForChannel(channelId: String) {
        viewModelScope.launch {
            modelRepository.observeEnabledModels(channelId).collectLatest { models ->
                _uiState.update { it.copy(models = models) }
                // 自动选择默认模型或第一个模型
                val channel = _uiState.value.currentChannel
                val defaultModelId = channel?.defaultModelId
                val selectedModel = models.find { it.id == defaultModelId } ?: models.firstOrNull()
                selectedModel?.let { selectModel(it) }
            }
        }
    }

    fun selectModel(model: LlmModel) {
        _uiState.update { it.copy(currentModel = model) }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun addImageFromUri(uri: Uri) {
        viewModelScope.launch {
            val result = attachmentRepository.importImageFromUri(uri)
            if (result.isSuccess) {
                val image = result.getOrNull() ?: return@launch
                _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + image) }
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "导入图片失败") }
            }
        }
    }

    fun addImageFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val result = attachmentRepository.importImageFromBitmap(bitmap)
            if (result.isSuccess) {
                val image = result.getOrNull() ?: return@launch
                _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + image) }
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "保存拍照图片失败") }
            }
        }
    }

    fun addFileFromUri(uri: Uri) {
        viewModelScope.launch {
            val result = attachmentRepository.importFileFromUri(uri)
            if (result.isSuccess) {
                val file = result.getOrNull() ?: return@launch
                _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + file) }
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "导入文件失败") }
            }
        }
    }

    fun addImageFromUrl(url: String) {
        val result = attachmentRepository.createImageFromUrl(url)
        if (result.isSuccess) {
            val image = result.getOrNull() ?: return
            _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + image) }
        } else {
            _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "添加 URL 失败") }
        }
    }

    fun removePendingAttachment(index: Int) {
        _uiState.update { state ->
            if (index !in state.pendingAttachments.indices) state
            else state.copy(pendingAttachments = state.pendingAttachments.toMutableList().also { it.removeAt(index) })
        }
    }

    fun setImageGenModel(modelId: String?) {
        val channel = _uiState.value.currentChannel ?: return
        viewModelScope.launch {
            channelRepository.updateChannel(
                channel.copy(
                    imageGenModelId = modelId,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun createNewConversation() {
        val channel = _uiState.value.currentChannel ?: return
        val model = _uiState.value.currentModel

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val conversation = Conversation(
                id = UUID.randomUUID().toString(),
                channelId = channel.id,
                modelId = model?.id,
                title = "新对话",
                systemPrompt = null,
                parentMessageId = null,
                rootConversationId = null,
                lastMessageTime = now,
                createdAt = now
            )
            conversationRepository.createConversation(conversation)
            loadConversation(conversation.id)
        }
    }

    fun loadConversation(conversationId: String) {
        conversationObserverJob?.cancel()
        messageObserverJob?.cancel()

        conversationObserverJob = viewModelScope.launch {
            conversationRepository.observeConversationById(conversationId)
                .filterNotNull()
                .collectLatest { conversation ->
                    _uiState.update { it.copy(currentConversation = conversation) }
                    if (_uiState.value.currentChannel?.id != conversation.channelId) {
                        selectChannel(conversation.channelId)
                    }
                }
        }

        messageObserverJob = viewModelScope.launch {
            messageRepository.observeMessagesByConversation(conversationId).collectLatest { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private suspend fun waitForConversationReady(conversationId: String): Conversation {
        return conversationRepository.observeConversationById(conversationId)
            .filterNotNull()
            .first()
    }

    fun sendMessage() {
        val currentState = _uiState.value
        if (currentState.inputText.isBlank() && currentState.pendingAttachments.isEmpty()) return

        val outgoing = buildOutgoingContent(currentState.inputText, currentState.pendingAttachments)

        if (currentState.currentConversation == null) {
            createNewConversationAndSend(outgoing)
            return
        }

        val conversationId = currentState.currentConversation.id
        sendMessageInternal(conversationId, outgoing)
    }

    private fun buildOutgoingContent(text: String, attachments: List<ContentPart>): List<ContentPart> {
        val parts = mutableListOf<ContentPart>()
        if (text.isNotBlank()) parts.add(ContentPart.Text(text))
        parts.addAll(attachments.filter { it !is ContentPart.Text })
        return parts
    }

    private fun sendMessageInternal(conversationId: String, content: List<ContentPart>) {
        val channel = _uiState.value.currentChannel ?: return

        _uiState.update {
            it.copy(
                inputText = "",
                pendingAttachments = emptyList(),
                isLoading = true,
                isStreaming = false,
                streamingContent = "",
                streamingThinking = "",
                error = null
            )
        }

        sendMessageJob?.cancel()
        sendMessageJob = viewModelScope.launch {
            try {
                sendMessageUseCase(
                    SendMessageUseCase.Params(
                        conversationId = conversationId,
                        content = content,
                        streamEnabled = channel.streamEnabled
                    )
                ).collect { event ->
                    handleStreamEvent(event)
                }
            } catch (e: CancellationException) {
                // 用户主动取消，不提示错误
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "发送失败",
                        isLoading = false,
                        isStreaming = false
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isStreaming = false,
                        streamingContent = "",
                        streamingThinking = ""
                    )
                }
            }
        }
    }

    private fun createNewConversationAndSend(outgoing: List<ContentPart>) {
        val channel = _uiState.value.currentChannel ?: return
        val model = _uiState.value.currentModel

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val title = outgoing.filterIsInstance<ContentPart.Text>()
                .firstOrNull()
                ?.text
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.take(30)
                ?: outgoing.filterIsInstance<ContentPart.File>().firstOrNull()?.fileName?.take(30)
                ?: "新对话"
            val conversation = Conversation(
                id = UUID.randomUUID().toString(),
                channelId = channel.id,
                modelId = model?.id,
                title = title,
                systemPrompt = null,
                parentMessageId = null,
                rootConversationId = null,
                lastMessageTime = now,
                createdAt = now
            )
            conversationRepository.createConversation(conversation)
            loadConversation(conversation.id)
            waitForConversationReady(conversation.id)
            sendMessageInternal(conversation.id, outgoing)
        }
    }

    fun generateImage(prompt: String) {
        val currentConversation = _uiState.value.currentConversation
        if (currentConversation == null) {
            createNewConversationAndGenerateImage(prompt)
            return
        }
        generateImageInternal(currentConversation.id, prompt)
    }

    private fun createNewConversationAndGenerateImage(prompt: String) {
        val channel = _uiState.value.currentChannel ?: return
        val model = _uiState.value.currentModel

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val title = "生图：${prompt.trim()}".take(30).ifBlank { "新对话" }
            val conversation = Conversation(
                id = UUID.randomUUID().toString(),
                channelId = channel.id,
                modelId = model?.id,
                title = title,
                systemPrompt = null,
                parentMessageId = null,
                rootConversationId = null,
                lastMessageTime = now,
                createdAt = now
            )
            conversationRepository.createConversation(conversation)
            loadConversation(conversation.id)
            waitForConversationReady(conversation.id)
            generateImageInternal(conversation.id, prompt)
        }
    }

    private fun generateImageInternal(conversationId: String, prompt: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        sendMessageJob?.cancel()
        sendMessageJob = viewModelScope.launch {
            try {
                generateImageUseCase(
                    GenerateImageUseCase.Params(
                        conversationId = conversationId,
                        prompt = prompt
                    )
                ).getOrThrow()
            } catch (e: CancellationException) {
                // 用户主动取消，不提示错误
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "生成图片失败") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleStreamEvent(event: StreamEvent) {
        when (event) {
            is StreamEvent.TextDelta -> {
                _uiState.update {
                    it.copy(
                        isStreaming = true,
                        streamingContent = it.streamingContent + event.text
                    )
                }
            }
            is StreamEvent.ThinkingDelta -> {
                _uiState.update {
                    it.copy(
                        isStreaming = true,
                        streamingThinking = it.streamingThinking + event.text
                    )
                }
            }
            is StreamEvent.ImageGenerated -> {
                viewModelScope.launch {
                    val conversation = _uiState.value.currentConversation ?: return@launch
                    val saved = attachmentRepository.saveGeneratedImage(event.base64, event.mimeType)
                    if (saved.isFailure) {
                        _uiState.update { it.copy(error = saved.exceptionOrNull()?.message ?: "保存生成图片失败") }
                        return@launch
                    }
                    val imagePart = saved.getOrNull() ?: return@launch
                    val message = Message(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversation.id,
                        parentId = null,
                        role = MessageRole.ASSISTANT,
                        content = listOf(imagePart),
                        thinking = null,
                        thinkingCollapsed = true,
                        modelUsed = _uiState.value.currentModel?.id,
                        tokenCount = null,
                        isEdited = false,
                        editedAt = null,
                        createdAt = System.currentTimeMillis()
                    )
                    messageRepository.addMessage(message)
                    conversationRepository.updateConversation(conversation.copy(lastMessageTime = message.createdAt))
                }
            }
            is StreamEvent.Error -> {
                _uiState.update {
                    it.copy(
                        error = "${event.code}: ${event.message}",
                        isLoading = false,
                        isStreaming = false
                    )
                }
            }
            StreamEvent.Done -> {
                _uiState.update {
                    it.copy(isStreaming = false)
                }
            }
        }
    }

    fun cancelGeneration() {
        sendMessageJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = false,
                isStreaming = false
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun editMessage(message: Message, newText: String) {
        val channel = _uiState.value.currentChannel ?: return
        val conversationId = message.conversationId
        val newContent = listOf(ContentPart.Text(newText))

        _uiState.update {
            it.copy(
                isLoading = true,
                isStreaming = false,
                streamingContent = "",
                streamingThinking = "",
                error = null
            )
        }

        sendMessageJob?.cancel()
        sendMessageJob = viewModelScope.launch {
            try {
                editMessageUseCase(
                    EditMessageUseCase.Params(
                        conversationId = conversationId,
                        messageId = message.id,
                        newContent = newContent,
                        streamEnabled = channel.streamEnabled
                    )
                ).collect { event ->
                    handleStreamEvent(event)
                }
            } catch (e: CancellationException) {
                // 用户主动取消，不提示错误
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "编辑失败",
                        isLoading = false,
                        isStreaming = false
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isStreaming = false,
                        streamingContent = "",
                        streamingThinking = ""
                    )
                }
            }
        }
    }

    fun regenerateResponse(targetAssistantMessage: Message) {
        val channel = _uiState.value.currentChannel ?: return

        _uiState.update {
            it.copy(
                isLoading = true,
                isStreaming = false,
                streamingContent = "",
                streamingThinking = "",
                error = null
            )
        }

        sendMessageJob?.cancel()
        sendMessageJob = viewModelScope.launch {
            try {
                val textBuilder = StringBuilder()
                val thinkingBuilder = StringBuilder()

                regenerateResponseUseCase(targetAssistantMessage.id).collect { event ->
                    when (event) {
                        is StreamEvent.TextDelta -> textBuilder.append(event.text)
                        is StreamEvent.ThinkingDelta -> thinkingBuilder.append(event.text)
                        else -> {}
                    }
                    handleStreamEvent(event)
                }

                val assistantMessage = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = targetAssistantMessage.conversationId,
                    parentId = targetAssistantMessage.parentId,
                    role = com.example.charisha.domain.model.MessageRole.ASSISTANT,
                    content = listOf(ContentPart.Text(textBuilder.toString())),
                    thinking = thinkingBuilder.toString().takeIf { it.isNotEmpty() },
                    thinkingCollapsed = true,
                    modelUsed = _uiState.value.currentModel?.id,
                    tokenCount = null,
                    isEdited = false,
                    editedAt = null,
                    createdAt = System.currentTimeMillis()
                )
                messageRepository.addMessage(assistantMessage)

                _uiState.value.currentConversation?.let { conversation ->
                    if (conversation.id == assistantMessage.conversationId) {
                        conversationRepository.updateConversation(
                            conversation.copy(lastMessageTime = assistantMessage.createdAt)
                        )
                    }
                }
            } catch (e: CancellationException) {
                // 用户主动取消，不提示错误
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "重新生成失败",
                        isLoading = false,
                        isStreaming = false
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isStreaming = false,
                        streamingContent = "",
                        streamingThinking = ""
                    )
                }
            }
        }
    }

    fun createBranch(fromMessageId: String) {
        viewModelScope.launch {
            val result = createBranchUseCase(fromMessageId)
            if (result.isSuccess) {
                val conversation = result.getOrNull() ?: return@launch
                loadConversation(conversation.id)
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "创建分支失败") }
            }
        }
    }
}
