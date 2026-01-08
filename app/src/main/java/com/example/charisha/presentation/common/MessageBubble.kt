package com.example.charisha.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole

/**
 * 消息气泡组件 - 支持 Message 对象（多模态内容）
 */
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    MessageBubbleContent(
        role = message.role,
        contentParts = message.content,
        thinking = message.thinking,
        hasThinking = message.hasThinking,
        isStreaming = false,
        modifier = modifier
    )
}

/**
 * 消息气泡组件 - 支持直接传入 role 和 content（用于流式显示）
 */
@Composable
fun MessageBubble(
    role: MessageRole,
    content: String,
    modifier: Modifier = Modifier,
    thinking: String? = null,
    isStreaming: Boolean = false
) {
    val textContent = if (content.isNotEmpty()) {
        content + if (isStreaming) "▌" else ""
    } else {
        ""
    }
    val contentParts = if (textContent.isNotEmpty()) {
        listOf(ContentPart.Text(textContent))
    } else {
        emptyList()
    }
    MessageBubbleContent(
        role = role,
        contentParts = contentParts,
        thinking = thinking,
        hasThinking = !thinking.isNullOrEmpty(),
        isStreaming = isStreaming,
        modifier = modifier
    )
}

@Composable
private fun MessageBubbleContent(
    role: MessageRole,
    contentParts: List<ContentPart>,
    thinking: String?,
    hasThinking: Boolean,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val isUser = role == MessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val configuration = LocalConfiguration.current
    val maxBubbleWidth = (configuration.screenWidthDp * 0.85).dp
    val normalizedParts = normalizeContentParts(contentParts)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Column {
                // 思维链内容（仅 AI 消息）
                if (hasThinking && !isUser) {
                    ThinkingBlock(
                        content = thinking ?: "",
                        isStreaming = isStreaming,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 正文内容（支持多模态）
                if (normalizedParts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        normalizedParts.forEach { part ->
                            when (part) {
                                is ContentPart.Text -> {
                                    if (part.text.isNotEmpty()) {
                                        MarkdownText(
                                            markdown = part.text,
                                            color = contentColor
                                        )
                                    }
                                }
                                is ContentPart.Image -> {
                                    MessageImage(image = part)
                                }
                                is ContentPart.File -> {
                                    FileAttachment(file = part)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 合并连续的 Text 内容，保持其他类型顺序
 * 使用换行符保留段落边界
 */
private fun normalizeContentParts(contentParts: List<ContentPart>): List<ContentPart> {
    if (contentParts.isEmpty()) return emptyList()

    val normalized = mutableListOf<ContentPart>()
    val textParts = mutableListOf<String>()

    fun flushText() {
        if (textParts.isNotEmpty()) {
            normalized.add(ContentPart.Text(textParts.joinToString("\n\n")))
            textParts.clear()
        }
    }

    contentParts.forEach { part ->
        when (part) {
            is ContentPart.Text -> {
                if (part.text.isNotEmpty()) {
                    textParts.add(part.text)
                }
            }
            else -> {
                flushText()
                normalized.add(part)
            }
        }
    }
    flushText()
    return normalized
}

@Composable
private fun FileAttachment(file: ContentPart.File) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "文件",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.mimeType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
