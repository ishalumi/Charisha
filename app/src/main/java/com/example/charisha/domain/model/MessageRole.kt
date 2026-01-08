package com.example.charisha.domain.model

/**
 * 消息角色
 */
enum class MessageRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): MessageRole =
            entries.find { it.value == value } ?: UNKNOWN
    }
}
