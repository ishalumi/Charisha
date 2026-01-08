package com.example.charisha.data.repository

import com.example.charisha.data.local.dao.ChannelDao
import com.example.charisha.data.local.dao.ModelDao
import com.example.charisha.data.local.mapper.EntityMapper.toDomain
import com.example.charisha.data.local.mapper.EntityMapper.toEntity
import com.example.charisha.data.local.prefs.SecurePreferences
import com.example.charisha.data.remote.ApiUrl
import com.example.charisha.data.remote.api.ClaudeApi
import com.example.charisha.data.remote.api.GeminiApi
import com.example.charisha.data.remote.api.OpenAIApi
import com.example.charisha.domain.model.LlmModel
import com.example.charisha.domain.model.ProviderType
import com.example.charisha.domain.repository.ModelInfo
import com.example.charisha.domain.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    private val modelDao: ModelDao,
    private val channelDao: ChannelDao,
    private val securePreferences: SecurePreferences,
    private val retrofitBuilder: Retrofit.Builder
) : ModelRepository {

    override fun observeModelsByChannel(channelId: String): Flow<List<LlmModel>> =
        modelDao.observeByChannel(channelId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeModelById(id: String): Flow<LlmModel?> {
        throw UnsupportedOperationException("Use observeModelsByChannel and filter by id")
    }

    override fun observeEnabledModels(channelId: String): Flow<List<LlmModel>> =
        modelDao.observeEnabledByChannel(channelId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveModels(channelId: String, models: List<LlmModel>): Result<Unit> =
        runCatching {
            val entities = models.map { it.toEntity() }
            modelDao.replaceModelsForChannel(channelId, entities)
        }

    override suspend fun updateModel(model: LlmModel): Result<Unit> = runCatching {
        modelDao.update(model.toEntity())
    }

    override suspend fun deleteModelsByChannel(channelId: String): Result<Unit> = runCatching {
        modelDao.deleteByChannel(channelId)
    }

    override suspend fun fetchModelsFromApi(channelId: String): Result<List<ModelInfo>> =
        runCatching {
            val channel = channelDao.getById(channelId)
                ?: throw IllegalArgumentException("渠道不存在: $channelId")

            val apiKey = securePreferences.getApiKey(channel.apiKeyRef)
                ?: throw IllegalStateException("API Key 未配置")

            val providerType = ProviderType.fromValue(channel.providerType)
            val retrofit = retrofitBuilder.baseUrl(ApiUrl.normalizeProviderBaseUrl(providerType, channel.baseUrl)).build()

            when (providerType) {
                ProviderType.OPENAI -> fetchOpenAIModels(retrofit, apiKey, channel.modelsApiPath)
                ProviderType.GEMINI -> fetchGeminiModels(retrofit, apiKey)
                ProviderType.CLAUDE -> fetchClaudeModels(retrofit, apiKey)
                ProviderType.UNKNOWN -> throw IllegalArgumentException("未知提供商")
            }
        }

    private suspend fun fetchOpenAIModels(
        retrofit: Retrofit,
        apiKey: String,
        modelsApiPath: String?
    ): List<ModelInfo> {
        val api = retrofit.create(OpenAIApi::class.java)
        val path = ApiUrl.normalizePath(modelsApiPath, "models")
        val response = api.listModelsAt(OpenAIApi.buildAuthHeader(apiKey), path)

        if (!response.isSuccessful) {
            throw RuntimeException("获取模型列表失败: HTTP ${response.code()}")
        }

        return response.body()?.data?.map { model ->
            ModelInfo(
                id = model.id,
                displayName = null,
                created = model.created,
                ownedBy = model.ownedBy,
                contextLength = null,
                maxOutputTokens = null
            )
        } ?: emptyList()
    }

    private suspend fun fetchGeminiModels(retrofit: Retrofit, apiKey: String): List<ModelInfo> {
        val api = retrofit.create(GeminiApi::class.java)
        val response = api.listModels(apiKey)

        if (!response.isSuccessful) {
            throw RuntimeException("获取模型列表失败: HTTP ${response.code()}")
        }

        return response.body()?.models?.map { model ->
            val modelId = model.name.removePrefix("models/")
            ModelInfo(
                id = modelId,
                displayName = model.displayName,
                created = null,
                ownedBy = null,
                contextLength = model.inputTokenLimit,
                maxOutputTokens = model.outputTokenLimit
            )
        } ?: emptyList()
    }

    private suspend fun fetchClaudeModels(retrofit: Retrofit, apiKey: String): List<ModelInfo> {
        val api = retrofit.create(ClaudeApi::class.java)
        val allModels = mutableListOf<ModelInfo>()
        var afterId: String? = null

        do {
            val response = api.listModels(
                apiKey = apiKey,
                limit = 100,
                afterId = afterId
            )

            if (!response.isSuccessful) {
                throw RuntimeException("获取模型列表失败: HTTP ${response.code()}")
            }

            val body = response.body() ?: break

            allModels.addAll(body.data.map { model ->
                ModelInfo(
                    id = model.id,
                    displayName = model.displayName,
                    created = model.createdAt?.let { parseIso8601ToTimestamp(it) },
                    ownedBy = null,
                    contextLength = null,
                    maxOutputTokens = null
                )
            })

            afterId = if (body.hasMore) body.lastId else null
        } while (afterId != null)

        return allModels
    }

    private fun parseIso8601ToTimestamp(iso8601: String): Long? {
        return try {
            java.time.Instant.parse(iso8601).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

}
