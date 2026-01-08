package com.example.charisha.presentation.common

import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.charisha.domain.model.ContentPart
import java.io.File

private const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
private const val PREVIEW_MAX_SIZE_PX = 480

/**
 * 图片渲染组件 - 支持 URL、Base64、本地文件
 */
@Composable
fun MessageImage(
    image: ContentPart.Image,
    modifier: Modifier = Modifier,
    enableFullScreen: Boolean = true,
    previewMaxHeight: Dp = 240.dp
) {
    val context = LocalContext.current
    val imageData = remember(image) { resolveImageData(image) }
    var showViewer by remember { mutableStateOf(false) }

    if (imageData == null) {
        MessageImageError(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        return
    }

    val previewRequest = remember(imageData) {
        ImageRequest.Builder(context)
            .data(imageData)
            .size(PREVIEW_MAX_SIZE_PX)
            .crossfade(true)
            .build()
    }

    val fullScreenRequest = remember(imageData) {
        ImageRequest.Builder(context)
            .data(imageData)
            .crossfade(true)
            .build()
    }

    val imageModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 120.dp, max = previewMaxHeight)
        .clip(RoundedCornerShape(8.dp))
        .let {
            if (enableFullScreen) {
                it.clickable { showViewer = true }
            } else {
                it
            }
        }

    SubcomposeAsyncImage(
        model = previewRequest,
        contentDescription = "消息图片",
        contentScale = ContentScale.Fit,
        modifier = imageModifier,
        loading = { MessageImageLoading() },
        error = { MessageImageError(modifier = Modifier.fillMaxSize()) }
    )

    if (showViewer) {
        FullScreenImageDialog(
            imageRequest = fullScreenRequest,
            onDismiss = { showViewer = false }
        )
    }
}

@Composable
private fun MessageImageLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(8.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MessageImageError(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "图片加载失败",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FullScreenImageDialog(
    imageRequest: ImageRequest,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "全屏图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}

/**
 * 解析图片数据源
 * 优先级: localPath > sourceUrl (仅 http/https) > base64
 */
private fun resolveImageData(image: ContentPart.Image): Any? {
    val localPath = image.localPath?.takeIf { it.isNotBlank() }
    if (localPath != null) {
        val file = File(localPath)
        if (file.exists()) {
            return file
        }
    }

    val sourceUrl = image.sourceUrl?.takeIf { it.isNotBlank() }
    if (sourceUrl != null && isValidHttpUrl(sourceUrl)) {
        return sourceUrl
    }

    val base64 = image.base64?.trim().orEmpty()
    if (base64.isNotEmpty()) {
        return decodeBase64Image(base64, MAX_IMAGE_BYTES)
    }

    return null
}

private fun isValidHttpUrl(url: String): Boolean {
    val scheme = Uri.parse(url).scheme?.lowercase()
    return scheme == "http" || scheme == "https"
}

private fun decodeBase64Image(base64: String, maxBytes: Int): ByteArray? {
    val payload = if (base64.startsWith("data:", ignoreCase = true)) {
        base64.substringAfter(",", missingDelimiterValue = "")
    } else {
        base64
    }
    if (payload.isBlank()) return null

    val estimatedBytes = (payload.length * 3L) / 4L
    if (estimatedBytes > maxBytes) return null

    return try {
        val decoded = Base64.decode(payload, Base64.DEFAULT)
        if (decoded.size > maxBytes) null else decoded
    } catch (e: IllegalArgumentException) {
        null
    }
}
