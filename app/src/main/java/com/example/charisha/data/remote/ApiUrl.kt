package com.example.charisha.data.remote

import android.net.Uri
import com.example.charisha.domain.model.ProviderType

/**
 * API URL 规范化工具
 *
 * 目标：
 * - 允许用户填入“根域名”或“带版本段”的 baseUrl
 * - 在内部统一为“带版本段且以 / 结尾”的 baseUrl
 *
 * 示例：
 * - OpenAI: https://api.openai.com -> https://api.openai.com/v1/
 * - OpenAI: https://xxx/newapi/v1 -> https://xxx/newapi/v1/
 * - Gemini: https://generativelanguage.googleapis.com -> https://generativelanguage.googleapis.com/v1beta/
 * - Claude: https://api.anthropic.com -> https://api.anthropic.com/v1/
 */
object ApiUrl {

    fun normalizeProviderBaseUrl(providerType: ProviderType, baseUrl: String): String {
        val raw = baseUrl.trim().trimEnd('/')
        require(raw.isNotBlank()) { "Base URL 不能为空" }

        val expectedVersionSegment = when (providerType) {
            ProviderType.OPENAI -> "v1"
            ProviderType.GEMINI -> "v1beta"
            ProviderType.CLAUDE -> "v1"
            ProviderType.UNKNOWN -> null
        }

        val normalized = if (expectedVersionSegment == null) {
            raw
        } else {
            val uri = Uri.parse(raw)
            val segments = uri.pathSegments ?: emptyList()
            if (segments.any { it.equals(expectedVersionSegment, ignoreCase = true) }) {
                raw
            } else {
                // 不强行改写 path，仅在末尾补版本段（兼容 newapi/openai-compatible）
                "$raw/$expectedVersionSegment"
            }
        }

        return ensureTrailingSlash(normalized)
    }

    fun normalizePath(path: String?, defaultPath: String): String {
        val p = (path?.trim()).orEmpty().ifBlank { defaultPath }
        return p.trim().trimStart('/')
    }

    private fun ensureTrailingSlash(url: String): String = if (url.endsWith("/")) url else "$url/"
}

