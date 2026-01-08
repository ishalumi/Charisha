package com.example.charisha.data.provider

/**
 * Provider 能力描述
 */
data class ProviderCapabilities(
    val supportsVision: Boolean,
    val supportsPdf: Boolean,
    val supportsFileInlineData: Boolean,
    val supportsStreaming: Boolean,
    val supportsStreamingSseAlt: Boolean = false,
    val supportsImageGeneration: Boolean = false
)

