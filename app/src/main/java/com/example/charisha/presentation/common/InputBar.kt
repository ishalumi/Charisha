package com.example.charisha.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.charisha.domain.model.ContentPart

@Composable
fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onStopClick: (() -> Unit)? = null,
    hint: String = "输入消息...",
    attachments: List<ContentPart> = emptyList(),
    onAttachmentClick: (() -> Unit)? = null,
    onRemoveAttachment: ((Int) -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            if (attachments.isNotEmpty() && onRemoveAttachment != null) {
                AttachmentPreview(
                    attachments = attachments,
                    onRemove = onRemoveAttachment
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // 附件按钮
                IconButton(
                    onClick = { onAttachmentClick?.invoke() },
                    enabled = enabled && !isLoading && onAttachmentClick != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加附件",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 输入框
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(hint) },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    enabled = enabled && !isLoading
                )

                Spacer(modifier = Modifier.width(8.dp))

                val canSend = (value.isNotBlank() || attachments.isNotEmpty()) && enabled

                // 发送/停止按钮
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                isLoading && onStopClick != null -> MaterialTheme.colorScheme.error
                                canSend -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape
                        )
                ) {
                    if (isLoading && onStopClick != null) {
                        // 停止按钮
                        IconButton(onClick = onStopClick) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "停止生成",
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    } else if (isLoading) {
                        // 加载指示器
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // 发送按钮
                        IconButton(
                            onClick = onSendClick,
                            enabled = canSend
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "发送",
                                tint = if (canSend)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
