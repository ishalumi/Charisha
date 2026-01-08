package com.example.charisha.domain.model

/**
 * 渠道配置 - API 端点信息
 */
data class Channel(
    val id: String,
    val name: String,
    val providerType: ProviderType,
    val baseUrl: String,
    val apiKeyRef: String,
    val customHeaders: Map<String, String>? = null,
    val proxyType: ProxyType = ProxyType.NONE,
    val proxyHost: String? = null,
    val proxyPort: Int? = null,
    val defaultModelId: String? = null,
    val streamEnabled: Boolean = true,
    val imageGenModelId: String? = null,
    val modelsApiPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    val isProxyConfigured: Boolean
        get() = proxyType != ProxyType.NONE && proxyHost != null && proxyPort != null
}
