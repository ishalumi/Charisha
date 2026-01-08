package com.example.charisha.data.repository

import com.example.charisha.data.local.dao.ChannelDao
import com.example.charisha.data.local.mapper.EntityMapper.toDomain
import com.example.charisha.data.local.mapper.EntityMapper.toEntity
import com.example.charisha.data.local.prefs.SecurePreferences
import com.example.charisha.data.remote.ApiUrl
import com.example.charisha.data.remote.api.ClaudeApi
import com.example.charisha.data.remote.api.GeminiApi
import com.example.charisha.data.remote.api.OpenAIApi
import com.example.charisha.domain.model.Channel
import com.example.charisha.domain.model.ProviderType
import com.example.charisha.domain.repository.ChannelRepository
import com.example.charisha.domain.repository.ConnectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao,
    private val securePreferences: SecurePreferences,
    private val json: Json,
    private val retrofitBuilder: Retrofit.Builder
) : ChannelRepository {

    override fun observeChannels(): Flow<List<Channel>> =
        channelDao.observeAll().map { entities ->
            entities.map { entity ->
                val customHeaders = entity.customHeadersJson?.let {
                    json.decodeFromString<Map<String, String>>(it)
                }
                entity.toDomain(customHeaders)
            }
        }

    override fun observeChannelById(id: String): Flow<Channel?> =
        channelDao.observeById(id).map { entity ->
            entity?.let {
                val customHeaders = it.customHeadersJson?.let { headersJson ->
                    json.decodeFromString<Map<String, String>>(headersJson)
                }
                it.toDomain(customHeaders)
            }
        }

    override suspend fun createChannel(channel: Channel): Result<Unit> = runCatching {
        channelDao.insert(channel.toEntity(json))
    }

    override suspend fun updateChannel(channel: Channel): Result<Unit> = runCatching {
        channelDao.update(channel.toEntity(json))
    }

    override suspend fun deleteChannel(id: String): Result<Unit> = runCatching {
        securePreferences.deleteApiKey(id)
        channelDao.deleteById(id)
    }

    override suspend fun testConnection(channelId: String): Result<ConnectionStatus> = runCatching {
        val entity = channelDao.getById(channelId)
            ?: return@runCatching ConnectionStatus(
                isConnected = false,
                errorMessage = "渠道不存在"
            )

        val apiKey = securePreferences.getApiKey(entity.apiKeyRef)
            ?: return@runCatching ConnectionStatus(
                isConnected = false,
                errorMessage = "API Key 未配置"
            )

        val startTime = System.currentTimeMillis()
        val providerType = ProviderType.fromValue(entity.providerType)
        val retrofit = retrofitBuilder.baseUrl(ApiUrl.normalizeProviderBaseUrl(providerType, entity.baseUrl)).build()

        try {
            when (providerType) {
                ProviderType.OPENAI -> {
                    val api = retrofit.create(OpenAIApi::class.java)
                    val path = ApiUrl.normalizePath(entity.modelsApiPath, "models")
                    val response = api.listModelsAt(OpenAIApi.buildAuthHeader(apiKey), path)
                    if (response.isSuccessful) {
                        ConnectionStatus(
                            isConnected = true,
                            latencyMs = System.currentTimeMillis() - startTime
                        )
                    } else {
                        ConnectionStatus(
                            isConnected = false,
                            errorMessage = "HTTP ${response.code()}: ${response.message()}"
                        )
                    }
                }
                ProviderType.GEMINI -> {
                    val api = retrofit.create(GeminiApi::class.java)
                    val response = api.listModels(apiKey)
                    if (response.isSuccessful) {
                        ConnectionStatus(
                            isConnected = true,
                            latencyMs = System.currentTimeMillis() - startTime
                        )
                    } else {
                        ConnectionStatus(
                            isConnected = false,
                            errorMessage = "HTTP ${response.code()}: ${response.message()}"
                        )
                    }
                }
                ProviderType.CLAUDE -> {
                    val api = retrofit.create(ClaudeApi::class.java)
                    val response = api.listModels(apiKey, limit = 1)
                    if (response.isSuccessful) {
                        ConnectionStatus(
                            isConnected = true,
                            latencyMs = System.currentTimeMillis() - startTime
                        )
                    } else {
                        ConnectionStatus(
                            isConnected = false,
                            errorMessage = "HTTP ${response.code()}: ${response.message()}"
                        )
                    }
                }
                ProviderType.UNKNOWN -> ConnectionStatus(
                    isConnected = false,
                    errorMessage = "未知提供商"
                )
            }
        } catch (e: Exception) {
            ConnectionStatus(
                isConnected = false,
                errorMessage = e.message ?: "连接失败"
            )
        }
    }
}
