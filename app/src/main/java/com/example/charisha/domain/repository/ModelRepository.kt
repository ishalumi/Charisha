package com.example.charisha.domain.repository

import com.example.charisha.domain.model.LlmModel
import kotlinx.coroutines.flow.Flow

/**
 * 模型仓库接口
 */
interface ModelRepository {
    fun observeModelsByChannel(channelId: String): Flow<List<LlmModel>>
    fun observeModelById(id: String): Flow<LlmModel?>
    fun observeEnabledModels(channelId: String): Flow<List<LlmModel>>
    suspend fun saveModels(channelId: String, models: List<LlmModel>): Result<Unit>
    suspend fun updateModel(model: LlmModel): Result<Unit>
    suspend fun deleteModelsByChannel(channelId: String): Result<Unit>
    suspend fun fetchModelsFromApi(channelId: String): Result<List<ModelInfo>>
}

/**
 * API 返回的模型信息
 */
data class ModelInfo(
    val id: String,
    val displayName: String?,
    val created: Long?,
    val ownedBy: String?,
    val contextLength: Int?,
    val maxOutputTokens: Int?
)
