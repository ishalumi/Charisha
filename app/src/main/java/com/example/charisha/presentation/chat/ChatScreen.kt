package com.example.charisha.presentation.chat

import android.content.ClipboardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.Message
import com.example.charisha.presentation.common.InputBar
import com.example.charisha.presentation.common.LoadingIndicator
import com.example.charisha.presentation.common.BranchIndicator
import com.example.charisha.presentation.common.MessageBubble
import com.example.charisha.presentation.common.MessageEditDialog
import com.example.charisha.presentation.common.ThinkingBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String?,
    onNavigateToChannels: () -> Unit,
    onNavigateToConversations: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showImageUrlDialog by remember { mutableStateOf(false) }
    var showImageGenDialog by remember { mutableStateOf(false) }
    var showImageGenModelSheet by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        if (!conversationId.isNullOrBlank()) {
            viewModel.loadConversation(conversationId)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addImageFromUri(it) }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addFileFromUri(it) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addFileFromUri(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.addImageFromBitmap(it) }
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = clipboard?.primaryClip ?: return
        if (clip.itemCount <= 0) return

        val item = clip.getItemAt(0)
        val uri = item.uri
        if (uri != null) {
            val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
                ?: runCatching { clip.description.getMimeType(0) }.getOrNull()
                ?: ""
            if (mimeType.startsWith("image/")) {
                viewModel.addImageFromUri(uri)
            } else {
                viewModel.addFileFromUri(uri)
            }
            return
        }

        val text = runCatching { item.coerceToText(context)?.toString() }.getOrNull().orEmpty().trim()
        if (text.startsWith("http://") || text.startsWith("https://")) {
            viewModel.addImageFromUrl(text)
        }
    }

    // 自动滚动到底部
    LaunchedEffect(uiState.messages.size, uiState.streamingContent) {
        if (uiState.messages.isNotEmpty() || uiState.isStreaming) {
            listState.animateScrollToItem(
                maxOf(0, uiState.messages.size - 1 + if (uiState.isStreaming) 1 else 0)
            )
        }
    }

    // 显示错误
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.currentConversation?.title ?: "新对话",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // 显示当前渠道和模型
                        if (uiState.currentChannel != null || uiState.currentModel != null) {
                            Text(
                                text = buildString {
                                    uiState.currentChannel?.let { append(it.name) }
                                    uiState.currentModel?.let {
                                        if (isNotEmpty()) append(" • ")
                                        append(it.displayName)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        uiState.currentConversation?.rootConversationId?.let { rootId ->
                            BranchIndicator(
                                rootConversationId = rootId,
                                parentMessageId = uiState.currentConversation?.parentMessageId,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                actions = {
                    // 新建对话
                    IconButton(onClick = { viewModel.createNewConversation() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建对话")
                    }
                    // 对话历史
                    IconButton(onClick = onNavigateToConversations) {
                        Icon(Icons.Default.History, contentDescription = "对话历史")
                    }
                    // 渠道设置
                    IconButton(onClick = onNavigateToChannels) {
                        Icon(Icons.Default.Settings, contentDescription = "渠道设置")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 渠道和模型选择器
            if (uiState.channels.isNotEmpty()) {
                ChannelModelSelector(
                    channels = uiState.channels,
                    currentChannel = uiState.currentChannel,
                    models = uiState.models,
                    currentModel = uiState.currentModel,
                    onChannelSelect = { viewModel.selectChannel(it.id) },
                    onModelSelect = { viewModel.selectModel(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 消息列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 空状态提示
                if (uiState.messages.isEmpty() && !uiState.isStreaming && !uiState.isLoading) {
                    item {
                        EmptyStateHint(
                            hasChannel = uiState.currentChannel != null,
                            onNavigateToChannels = onNavigateToChannels
                        )
                    }
                }

                // 历史消息
                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { selectedMessage = message }
                            )
                    )
                }

                // 流式响应中的消息
                if (uiState.isStreaming || uiState.isLoading) {
                    item {
                        StreamingMessageBubble(
                            content = uiState.streamingContent,
                            thinking = uiState.streamingThinking,
                            isLoading = uiState.isLoading && !uiState.isStreaming
                        )
                    }
                }
            }

            // 输入区域
            InputBar(
                value = uiState.inputText,
                onValueChange = viewModel::onInputTextChanged,
                onSendClick = viewModel::sendMessage,
                enabled = uiState.currentChannel != null && !uiState.isLoading,
                isLoading = uiState.isLoading,
                onStopClick = if (uiState.isStreaming) viewModel::cancelGeneration else null,
                attachments = uiState.pendingAttachments,
                onAttachmentClick = { showAttachmentSheet = true },
                onRemoveAttachment = viewModel::removePendingAttachment,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false }
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "添加附件",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TextButton(
                    onClick = {
                        showAttachmentSheet = false
                        imagePickerLauncher.launch("image/*")
                    }
                ) { Text("从相册选择图片") }

                TextButton(
                    onClick = {
                        showAttachmentSheet = false
                        cameraLauncher.launch(null)
                    }
                ) { Text("拍照") }

                TextButton(
                    onClick = {
                        showAttachmentSheet = false
                        pasteFromClipboard()
                    }
                ) { Text("从剪贴板粘贴") }

                TextButton(
                    onClick = {
                        showAttachmentSheet = false
                        showImageUrlDialog = true
                    }
                ) { Text("添加图片 URL") }

                TextButton(
                    onClick = {
                        showAttachmentSheet = false
                        pdfPickerLauncher.launch("application/pdf")
                    }
                ) { Text("选择 PDF") }

                TextButton(
                    onClick = {
                        showAttachmentSheet = false
                        filePickerLauncher.launch("*/*")
                    }
                ) { Text("选择文件") }

                val imageGenModels = uiState.models.filter { it.supportsImageGen }
                if (imageGenModels.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            showAttachmentSheet = false
                            showImageGenModelSheet = true
                        }
                    ) { Text("选择生图模型") }
                }

                TextButton(
                    onClick = {
                        showAttachmentSheet = false
                        showImageGenDialog = true
                    },
                    enabled = uiState.currentConversation != null || uiState.currentChannel != null
                ) { Text("生成图片") }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showImageUrlDialog) {
        MessageEditDialog(
            title = "添加图片 URL",
            initialText = "",
            confirmText = "添加",
            onConfirm = { url ->
                viewModel.addImageFromUrl(url)
                showImageUrlDialog = false
            },
            onDismiss = { showImageUrlDialog = false }
        )
    }

    if (showImageGenDialog) {
        MessageEditDialog(
            title = "生成图片",
            initialText = "",
            confirmText = "生成",
            onConfirm = { prompt ->
                viewModel.generateImage(prompt)
                showImageGenDialog = false
            },
            onDismiss = { showImageGenDialog = false }
        )
    }

    if (showImageGenModelSheet) {
        val models = uiState.models.filter { it.supportsImageGen }
        ModalBottomSheet(
            onDismissRequest = { showImageGenModelSheet = false }
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "选择生图模型",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val current = uiState.currentChannel?.imageGenModelId
                Text(
                    text = if (current.isNullOrBlank()) "当前：跟随对话模型" else "当前：$current",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TextButton(
                    onClick = {
                        viewModel.setImageGenModel(null)
                        showImageGenModelSheet = false
                    }
                ) { Text("跟随对话模型") }

                models.forEach { model ->
                    TextButton(
                        onClick = {
                            viewModel.setImageGenModel(model.id)
                            showImageGenModelSheet = false
                        }
                    ) {
                        Text(model.displayName)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 消息长按操作面板
    selectedMessage?.let { message ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessage = null }
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = if (message.role == MessageRole.USER) "用户消息" else "AI 消息",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TextButton(
                    onClick = {
                        editingMessage = message
                        selectedMessage = null
                    },
                    enabled = message.canEdit && !uiState.isLoading
                ) {
                    Text("编辑")
                }

                if (message.role == MessageRole.ASSISTANT) {
                    TextButton(
                        onClick = {
                            viewModel.regenerateResponse(message)
                            selectedMessage = null
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Text("重新生成")
                    }

                    TextButton(
                        onClick = {
                            viewModel.createBranch(message.id)
                            selectedMessage = null
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Text("创建分支")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 消息编辑弹窗
    editingMessage?.let { message ->
        MessageEditDialog(
            title = if (message.role == MessageRole.USER) "编辑用户消息" else "编辑 AI 消息",
            initialText = message.textContent,
            onConfirm = { newText ->
                viewModel.editMessage(message, newText)
                editingMessage = null
            },
            onDismiss = { editingMessage = null }
        )
    }
}

@Composable
private fun ChannelModelSelector(
    channels: List<com.example.charisha.domain.model.Channel>,
    currentChannel: com.example.charisha.domain.model.Channel?,
    models: List<com.example.charisha.domain.model.LlmModel>,
    currentModel: com.example.charisha.domain.model.LlmModel?,
    onChannelSelect: (com.example.charisha.domain.model.Channel) -> Unit,
    onModelSelect: (com.example.charisha.domain.model.LlmModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var showChannelMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 渠道选择
        Box {
            OutlinedButton(
                onClick = { showChannelMenu = true },
                modifier = Modifier.width(140.dp)
            ) {
                Text(
                    text = currentChannel?.name ?: "选择渠道",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = showChannelMenu,
                onDismissRequest = { showChannelMenu = false }
            ) {
                channels.forEach { channel ->
                    DropdownMenuItem(
                        text = { Text(channel.name) },
                        onClick = {
                            onChannelSelect(channel)
                            showChannelMenu = false
                        }
                    )
                }
            }
        }

        // 模型选择
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { showModelMenu = true },
                enabled = models.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = currentModel?.displayName ?: "选择模型",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = showModelMenu,
                onDismissRequest = { showModelMenu = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(model.displayName)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (model.supportsVision) {
                                        Text(
                                            "视觉",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (model.supportsReasoning) {
                                        Text(
                                            "推理",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            onModelSelect(model)
                            showModelMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateHint(
    hasChannel: Boolean,
    onNavigateToChannels: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (hasChannel) "开始新对话" else "请先配置渠道",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasChannel) "在下方输入框中输入消息开始对话" else "点击右上角设置按钮添加 API 渠道",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!hasChannel) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateToChannels) {
                Text("前往设置")
            }
        }
    }
}

@Composable
private fun StreamingMessageBubble(
    content: String,
    thinking: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 思维链内容
        AnimatedVisibility(visible = thinking.isNotEmpty()) {
            ThinkingBlock(
                content = thinking,
                isStreaming = true,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 正文内容或加载指示器
        if (content.isNotEmpty()) {
            MessageBubble(
                role = MessageRole.ASSISTANT,
                content = content,
                isStreaming = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (isLoading) {
            LoadingIndicator(text = "思考中...")
        }
    }
}
