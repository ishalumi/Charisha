package com.example.charisha.domain.model

/**
 * LLM API 提供商类型
 */
enum class ProviderType(val value: String) {
    OPENAI("openai"),
    GEMINI("gemini"),
    CLAUDE("claude"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): ProviderType =
            entries.find { it.value == value } ?: UNKNOWN
    }
}
