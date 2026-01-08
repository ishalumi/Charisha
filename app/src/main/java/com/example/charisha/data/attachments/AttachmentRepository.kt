package com.example.charisha.data.attachments

import android.graphics.Bitmap
import android.net.Uri
import com.example.charisha.domain.model.ContentPart

/**
 * 附件导入与本地存储
 * - 将外部 Uri/拍照 Bitmap 导入到 APP 私有目录
 * - 生成可持久化的 ContentPart（通常仅保存 localPath，不保存 base64）
 */
interface AttachmentRepository {

    suspend fun importImageFromUri(uri: Uri): Result<ContentPart.Image>

    suspend fun importImageFromBitmap(bitmap: Bitmap): Result<ContentPart.Image>

    suspend fun importFileFromUri(uri: Uri): Result<ContentPart.File>

    fun createImageFromUrl(url: String): Result<ContentPart.Image>

    suspend fun saveGeneratedImage(base64: String, mimeType: String): Result<ContentPart.Image>
}
