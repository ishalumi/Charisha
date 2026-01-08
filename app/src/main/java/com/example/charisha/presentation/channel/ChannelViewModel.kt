package com.example.charisha.presentation.channel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charisha.domain.model.Channel
import com.example.charisha.domain.model.LlmModel
import com.example.charisha.domain.model.ProviderType
import com.example.charisha.domain.repository.ChannelRepository
import com.example.charisha.domain.repository.ConnectionStatus
import com.example.charisha.domain.repository.ModelRepository
import com.example.charisha.domain.usecase.channel.CreateChannelUseCase
import com.example.charisha.domain.usecase.channel.FetchModelsUseCase
import com.example.charisha.domain.usecase.channel.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val modelRepository: ModelRepository,
    private val createChannelUseCase: CreateChannelUseCase,
    private val fetchModelsUseCase: FetchModelsUseCase,
    private val testConnectionUseCase: TestConnectionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    private val editChannelId: String? = savedStateHandle["channelId"]

    init {
        loadChannels()
        editChannelId?.let { loadChannelForEdit(it) }
    }

    private fun loadChannels() {
        viewModelScope.launch {
            channelRepository.observeChannels().collectLatest { channels ->
                _uiState.update { it.copy(channels = channels) }
            }
        }
    }

    private fun loadChannelForEdit(channelId: String) {
        viewModelScope.launch {
            channelRepository.observeChannelById(channelId).collectLatest { channel ->
                channel?.let {
                    _uiState.update { state ->
                        state.copy(
                            editingChannel = channel,
                            streamEnabled = channel.streamEnabled
                        )
                    }
                    loadModelsForChannel(channelId)
                }
            }
        }
    }

    private fun loadModelsForChannel(channelId: String) {
        viewModelScope.launch {
            modelRepository.observeModelsByChannel(channelId).collectLatest { models ->
                _uiState.update { it.copy(models = models) }
            }
        }
    }

    fun onEvent(event: ChannelEvent) {
        when (event) {
            is ChannelEvent.DeleteChannel -> deleteChannel(event.channelId)
            is ChannelEvent.SaveChannel -> saveChannel(event)
            is ChannelEvent.TestConnection -> testConnection(event.channelId)
            is ChannelEvent.FetchModels -> fetchModels(event.channelId)
            is ChannelEvent.SetDefaultModel -> setDefaultModel(event.channelId, event.modelId)
            is ChannelEvent.ToggleStream -> toggleStream(event.enabled)
            is ChannelEvent.UpdateChannel -> updateChannel(event)
        }
    }

    private fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            channelRepository.deleteChannel(channelId)
        }
    }

    private fun saveChannel(event: ChannelEvent.SaveChannel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = createChannelUseCase(
                CreateChannelUseCase.Params(
                    name = event.name,
                    providerType = event.providerType,
                    baseUrl = event.baseUrl,
                    apiKey = event.apiKey,
                    streamEnabled = _uiState.value.streamEnabled,
                    modelsApiPath = event.modelsApiPath?.trim()?.takeIf { it.isNotBlank() }
                )
            )

            if (result.isSuccess) {
                val channel = result.getOrNull()!!
                // 保存成功后自动获取模型列表
                _uiState.update { it.copy(isFetchingModels = true) }
                val modelsResult = fetchModelsUseCase(channel.id)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFetchingModels = false,
                        isChannelSaved = true,
                        models = modelsResult.getOrDefault(emptyList()),
                        fetchModelsError = modelsResult.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    private fun updateChannel(event: ChannelEvent.UpdateChannel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val existingChannel = _uiState.value.editingChannel ?: return@launch
            val updatedChannel = existingChannel.copy(
                name = event.name,
                providerType = event.providerType,
                baseUrl = event.baseUrl.trimEnd('/'),
                streamEnabled = _uiState.value.streamEnabled,
                defaultModelId = event.defaultModelId,
                modelsApiPath = event.modelsApiPath?.trim()?.takeIf { it.isNotBlank() },
                updatedAt = System.currentTimeMillis()
            )

            val result = channelRepository.updateChannel(updatedChannel)

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isChannelSaved = true) }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    private fun testConnection(channelId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, connectionStatus = null) }

            val result = testConnectionUseCase(channelId)

            _uiState.update {
                it.copy(
                    isTesting = false,
                    connectionStatus = result.getOrNull(),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun fetchModels(channelId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true, fetchModelsError = null) }

            val result = fetchModelsUseCase(channelId)

            _uiState.update {
                it.copy(
                    isFetchingModels = false,
                    models = result.getOrDefault(it.models),
                    fetchModelsError = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun setDefaultModel(channelId: String, modelId: String) {
        viewModelScope.launch {
            val channel = _uiState.value.editingChannel ?: return@launch
            val updatedChannel = channel.copy(
                defaultModelId = modelId,
                updatedAt = System.currentTimeMillis()
            )
            channelRepository.updateChannel(updatedChannel)
            _uiState.update { it.copy(editingChannel = updatedChannel) }
        }
    }

    private fun toggleStream(enabled: Boolean) {
        _uiState.update { it.copy(streamEnabled = enabled) }
    }

    fun resetSaveState() {
        _uiState.update { it.copy(isChannelSaved = false, error = null) }
    }

    fun clearConnectionStatus() {
        _uiState.update { it.copy(connectionStatus = null) }
    }
}

data class ChannelUiState(
    val channels: List<Channel> = emptyList(),
    val editingChannel: Channel? = null,
    val models: List<LlmModel> = emptyList(),
    val isLoading: Boolean = false,
    val isTesting: Boolean = false,
    val isFetchingModels: Boolean = false,
    val connectionStatus: ConnectionStatus? = null,
    val streamEnabled: Boolean = true,
    val error: String? = null,
    val fetchModelsError: String? = null,
    val isChannelSaved: Boolean = false
)

sealed class ChannelEvent {
    data class DeleteChannel(val channelId: String) : ChannelEvent()
    data class SaveChannel(
        val name: String,
        val providerType: ProviderType,
        val baseUrl: String,
        val apiKey: String,
        val modelsApiPath: String? = null
    ) : ChannelEvent()
    data class UpdateChannel(
        val name: String,
        val providerType: ProviderType,
        val baseUrl: String,
        val defaultModelId: String?,
        val modelsApiPath: String? = null
    ) : ChannelEvent()
    data class TestConnection(val channelId: String) : ChannelEvent()
    data class FetchModels(val channelId: String) : ChannelEvent()
    data class SetDefaultModel(val channelId: String, val modelId: String) : ChannelEvent()
    data class ToggleStream(val enabled: Boolean) : ChannelEvent()
}
