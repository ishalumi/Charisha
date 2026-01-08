package com.example.charisha.domain.model

/**
 * 流式响应事件
 */
sealed interface StreamEvent {
    data class ThinkingDelta(val text: String) : StreamEvent
    data class TextDelta(val text: String) : StreamEvent
    data class ImageGenerated(val base64: String, val mimeType: String) : StreamEvent
    data class Error(val code: String, val message: String) : StreamEvent
    data object Done : StreamEvent
}
