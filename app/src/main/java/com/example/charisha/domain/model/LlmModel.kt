package com.example.charisha.domain.model

/**
 * LLM 模型配置
 */
data class LlmModel(
    val id: String,
    val channelId: String,
    val displayName: String,
    val contextLength: Int = 4096,
    val maxOutputTokens: Int = 4096,
    val supportsVision: Boolean = false,
    val supportsImageGen: Boolean = false,
    val supportsReasoning: Boolean = false,
    val supportsStreaming: Boolean = true,
    val defaultTemperature: Float = 0.7f,
    val defaultTopP: Float = 1.0f,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
) {
    val capabilities: ModelCapabilities
        get() = ModelCapabilities(
            vision = supportsVision,
            imageGen = supportsImageGen,
            reasoning = supportsReasoning,
            streaming = supportsStreaming
        )
}

/**
 * 模型能力标识
 */
data class ModelCapabilities(
    val vision: Boolean = false,
    val imageGen: Boolean = false,
    val reasoning: Boolean = false,
    val streaming: Boolean = true
)
