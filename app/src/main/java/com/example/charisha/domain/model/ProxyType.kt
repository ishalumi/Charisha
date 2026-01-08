package com.example.charisha.domain.model

/**
 * 代理类型
 */
enum class ProxyType(val value: String) {
    NONE("none"),
    HTTP("http"),
    SOCKS5("socks5"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): ProxyType =
            entries.find { it.value == value } ?: NONE
    }
}
