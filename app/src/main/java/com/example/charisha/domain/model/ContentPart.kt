package com.example.charisha.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 消息内容块 - 支持多模态
 */
@Serializable
sealed class ContentPart {

    @Serializable
    @SerialName("text")
    data class Text(
        val text: String
    ) : ContentPart()

    @Serializable
    @SerialName("image")
    data class Image(
        val localPath: String? = null,
        val base64: String? = null,
        val mimeType: String,
        val isGenerated: Boolean = false,
        val sourceUrl: String? = null
    ) : ContentPart()

    @Serializable
    @SerialName("file")
    data class File(
        val localPath: String,
        val fileName: String,
        val mimeType: String,
        val extractedText: String? = null
    ) : ContentPart()
}
