package com.example.charisha.data.provider

import android.net.Uri
import android.util.Base64
import com.example.charisha.domain.model.ContentPart
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * 将 ContentPart 的二进制来源统一解析为 base64
 */
class BinaryResolver(
    private val okHttpClient: OkHttpClient
) {

    fun resolveImage(image: ContentPart.Image, maxBytes: Long = DEFAULT_MAX_BYTES): ResolvedBinary {
        val sourceUrl = image.sourceUrl?.takeIf { it.isNotBlank() }
        if (sourceUrl != null) {
            val scheme = Uri.parse(sourceUrl).scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                throw IllegalArgumentException("仅支持 http/https 图片 URL")
            }
            val (bytes, mimeType) = downloadBytes(sourceUrl, maxBytes)
            return ResolvedBinary(mimeType = mimeType, base64 = bytes.toBase64NoWrap())
        }

        val localPath = image.localPath?.takeIf { it.isNotBlank() }
        if (localPath != null) {
            val file = File(localPath)
            if (!file.exists()) throw IllegalArgumentException("图片文件不存在")
            if (file.length() > maxBytes) throw IllegalArgumentException("图片超过大小限制")
            val bytes = file.readBytes()
            val mime = image.mimeType.takeIf { it.isNotBlank() && it != "image/*" } ?: "image/jpeg"
            return ResolvedBinary(mimeType = mime, base64 = bytes.toBase64NoWrap())
        }

        val base64 = image.base64?.trim().orEmpty()
        if (base64.isNotEmpty()) {
            val payload = if (base64.startsWith("data:", ignoreCase = true)) {
                base64.substringAfter(",", missingDelimiterValue = "")
            } else {
                base64
            }
            if (payload.isBlank()) throw IllegalArgumentException("Base64 图片为空")
            val decoded = runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull()
                ?: throw IllegalArgumentException("Base64 图片解码失败")
            if (decoded.size.toLong() > maxBytes) throw IllegalArgumentException("图片超过大小限制")
            val mime = image.mimeType.takeIf { it.isNotBlank() && it != "image/*" } ?: "image/jpeg"
            return ResolvedBinary(mimeType = mime, base64 = decoded.toBase64NoWrap())
        }

        throw IllegalArgumentException("图片内容为空")
    }

    fun resolveFile(file: ContentPart.File, maxBytes: Long = DEFAULT_MAX_BYTES): ResolvedBinary {
        val f = File(file.localPath)
        if (!f.exists()) throw IllegalArgumentException("文件不存在")
        if (f.length() > maxBytes) throw IllegalArgumentException("文件超过大小限制")
        val bytes = f.readBytes()
        val mime = file.mimeType.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        return ResolvedBinary(mimeType = mime, base64 = bytes.toBase64NoWrap())
    }

    private fun downloadBytes(url: String, maxBytes: Long): Pair<ByteArray, String> {
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalArgumentException("下载失败: HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalArgumentException("下载失败: 空响应")
            val contentLength = body.contentLength()
            if (contentLength > maxBytes) throw IllegalArgumentException("下载内容超过大小限制")
            val bytes = body.bytes()
            if (bytes.size.toLong() > maxBytes) throw IllegalArgumentException("下载内容超过大小限制")
            val mime = response.header("Content-Type")?.substringBefore(';')?.trim().orEmpty()
            val finalMime = if (mime.isNotBlank()) mime else "application/octet-stream"
            return bytes to finalMime
        }
    }

    private fun ByteArray.toBase64NoWrap(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    data class ResolvedBinary(
        val mimeType: String,
        val base64: String
    )

    private companion object {
        private const val DEFAULT_MAX_BYTES = 20L * 1024L * 1024L
    }
}

