package com.example.charisha.domain.usecase.channel

import com.example.charisha.domain.model.LlmModel
import com.example.charisha.domain.repository.ModelInfo
import com.example.charisha.domain.repository.ModelRepository
import javax.inject.Inject

/**
 * 获取模型列表用例
 */
class FetchModelsUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(channelId: String): Result<List<LlmModel>> = runCatching {
        val modelInfoList = modelRepository.fetchModelsFromApi(channelId).getOrThrow()

        val now = System.currentTimeMillis()
        val models = modelInfoList.mapIndexed { index, info ->
            val capabilities = inferCapabilities(info.id)
            LlmModel(
                id = info.id,
                channelId = channelId,
                displayName = info.displayName ?: info.id,
                contextLength = info.contextLength ?: 4096,
                maxOutputTokens = info.maxOutputTokens ?: 4096,
                supportsVision = capabilities.vision,
                supportsImageGen = capabilities.imageGen,
                supportsReasoning = capabilities.reasoning,
                supportsStreaming = capabilities.streaming,
                defaultTemperature = 0.7f,
                defaultTopP = 1.0f,
                isEnabled = true,
                sortOrder = index,
                createdAt = now,
                updatedAt = now
            )
        }

        modelRepository.saveModels(channelId, models).getOrThrow()
        models
    }

    private fun inferCapabilities(modelId: String): ModelCapabilities {
        val id = modelId.lowercase()
        return when {
            id.contains("gpt-4o") || id.contains("gpt-4-vision") ->
                ModelCapabilities(vision = true, streaming = true)
            id.startsWith("o1") || id.startsWith("o3") ->
                ModelCapabilities(reasoning = true, streaming = false)
            id.contains("dall-e") || id.contains("gpt-image") ->
                ModelCapabilities(imageGen = true, streaming = false)
            id.contains("claude") ->
                ModelCapabilities(vision = true, reasoning = id.contains("thinking"), streaming = true)
            id.contains("gemini-2") || id.contains("gemini-1.5") ->
                ModelCapabilities(vision = true, streaming = true)
            id.contains("imagen") ->
                ModelCapabilities(imageGen = true, streaming = false)
            else -> ModelCapabilities()
        }
    }

    private data class ModelCapabilities(
        val vision: Boolean = false,
        val imageGen: Boolean = false,
        val reasoning: Boolean = false,
        val streaming: Boolean = true
    )
}
