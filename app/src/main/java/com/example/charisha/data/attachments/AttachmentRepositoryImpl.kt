package com.example.charisha.data.attachments

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.util.Base64
import com.example.charisha.domain.model.ContentPart
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AttachmentRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    override suspend fun importImageFromUri(uri: Uri): Result<ContentPart.Image> = runCatching {
        withContext(Dispatchers.IO) {
            val bitmap = decodeBitmap(uri)
                ?: throw IllegalArgumentException("无法读取图片")

            saveBitmapAsJpeg(bitmap)
        }
    }

    override suspend fun importImageFromBitmap(bitmap: Bitmap): Result<ContentPart.Image> = runCatching {
        withContext(Dispatchers.IO) {
            saveBitmapAsJpeg(bitmap)
        }
    }

    override suspend fun importFileFromUri(uri: Uri): Result<ContentPart.File> = runCatching {
        withContext(Dispatchers.IO) {
            val displayName = queryDisplayName(uri) ?: "file-${UUID.randomUUID()}"
            val mimeType = contentResolver.getType(uri) ?: guessMimeTypeFromName(displayName) ?: "application/octet-stream"

            val targetFile = createAttachmentFile(displayName)
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            total += read.toLong()
                            if (total > MAX_FILE_BYTES) {
                                throw IllegalArgumentException("文件超过 ${MAX_FILE_BYTES / 1024 / 1024}MB 限制")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                } ?: throw IllegalArgumentException("无法读取文件")
            } catch (e: Exception) {
                runCatching { targetFile.delete() }
                throw e
            }

            val extractedText = if (mimeType.startsWith("text/") || mimeType == "application/json") {
                runCatching { readTextSafely(targetFile) }.getOrNull()
            } else {
                null
            }

            ContentPart.File(
                localPath = targetFile.absolutePath,
                fileName = displayName,
                mimeType = mimeType,
                extractedText = extractedText
            )
        }
    }

    override fun createImageFromUrl(url: String): Result<ContentPart.Image> = runCatching {
        val trimmed = url.trim()
        if (trimmed.isBlank()) throw IllegalArgumentException("URL 不能为空")
        val scheme = Uri.parse(trimmed).scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw IllegalArgumentException("仅支持 http/https 图片 URL")
        }

        ContentPart.Image(
            localPath = null,
            base64 = null,
            mimeType = "image/*",
            isGenerated = false,
            sourceUrl = trimmed
        )
    }

    override suspend fun saveGeneratedImage(base64: String, mimeType: String): Result<ContentPart.Image> = runCatching {
        withContext(Dispatchers.IO) {
            val payload = base64.trim().let {
                if (it.startsWith("data:", ignoreCase = true)) it.substringAfter(",", missingDelimiterValue = "") else it
            }
            if (payload.isBlank()) throw IllegalArgumentException("图片数据为空")
            val bytes = runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull()
                ?: throw IllegalArgumentException("图片数据解码失败")
            if (bytes.size.toLong() > MAX_GENERATED_IMAGE_BYTES) {
                throw IllegalArgumentException("生成图片超过 ${MAX_GENERATED_IMAGE_BYTES / 1024 / 1024}MB 限制")
            }

            val ext = when (mimeType.lowercase()) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/jpeg", "image/jpg" -> "jpg"
                else -> "png"
            }
            val targetFile = File(attachmentsDir(), "${System.currentTimeMillis()}-${UUID.randomUUID()}-gen.$ext")
            FileOutputStream(targetFile).use { it.write(bytes) }

            ContentPart.Image(
                localPath = targetFile.absolutePath,
                base64 = null,
                mimeType = mimeType.ifBlank { "image/$ext" },
                isGenerated = true,
                sourceUrl = null
            )
        }
    }

    private fun attachmentsDir(): File {
        val dir = File(context.filesDir, "attachments")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun createAttachmentFile(originalName: String): File {
        val safeName = originalName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return File(attachmentsDir(), "${System.currentTimeMillis()}-${UUID.randomUUID()}-$safeName")
    }

    private fun saveBitmapAsJpeg(bitmap: Bitmap): ContentPart.Image {
        val targetFile = File(attachmentsDir(), "${System.currentTimeMillis()}-${UUID.randomUUID()}.jpg")

        val scaled = scaleDown(bitmap, MAX_IMAGE_DIM_PX)
        val bytes = compressJpegToTargetSize(scaled, MAX_IMAGE_BYTES)

        FileOutputStream(targetFile).use { output ->
            output.write(bytes)
        }

        return ContentPart.Image(
            localPath = targetFile.absolutePath,
            base64 = null,
            mimeType = "image/jpeg",
            isGenerated = false,
            sourceUrl = null
        )
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        // 先读 bounds，计算采样率，再解码，避免 OOM
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        val (w, h) = bounds.outWidth to bounds.outHeight
        if (w <= 0 || h <= 0) return null

        val sampleSize = calculateInSampleSize(w, h, MAX_IMAGE_DIM_PX, MAX_IMAGE_DIM_PX)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        openStream(uri)?.use { input ->
            return BitmapFactory.decodeStream(input, null, options)
        }
        return null
    }

    private fun openStream(uri: Uri): InputStream? = contentResolver.openInputStream(uri)

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while (halfWidth / inSampleSize >= reqWidth && halfHeight / inSampleSize >= reqHeight) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = maxOf(width, height)
        if (maxSide <= maxDim) return bitmap

        val ratio = maxDim.toFloat() / maxSide.toFloat()
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun compressJpegToTargetSize(bitmap: Bitmap, maxBytes: Int): ByteArray {
        var quality = 92
        var last: ByteArray? = null

        while (quality >= 60) {
            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            last = bytes
            if (bytes.size <= maxBytes) return bytes
            quality -= 8
        }

        // 兜底：返回最后一次压缩结果（可能略超），交给上层/服务端报错
        return last?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("图片压缩失败")
    }

    private fun queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun guessMimeTypeFromName(name: String): String? {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase().takeIf { it.isNotBlank() }
            ?: return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun readTextSafely(file: File, maxChars: Int = 50_000): String {
        val sb = StringBuilder()
        file.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
            val buffer = CharArray(4096)
            while (sb.length < maxChars) {
                val read = reader.read(buffer, 0, minOf(buffer.size, maxChars - sb.length))
                if (read <= 0) break
                sb.append(buffer, 0, read)
            }
        }
        return sb.toString()
    }

    private companion object {
        private const val MAX_IMAGE_DIM_PX = 1536
        private const val MAX_IMAGE_BYTES = 2 * 1024 * 1024
        private const val MAX_FILE_BYTES = 20L * 1024L * 1024L
        private const val MAX_GENERATED_IMAGE_BYTES = 10L * 1024L * 1024L
    }
}
