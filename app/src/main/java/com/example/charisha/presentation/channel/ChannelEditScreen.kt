package com.example.charisha.presentation.channel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.charisha.domain.model.LlmModel
import com.example.charisha.domain.model.ProviderType
import com.example.charisha.presentation.common.StreamToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelEditScreen(
    channelId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditMode = channelId != null

    // 表单状态
    var name by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(ProviderType.OPENAI) }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedModelId by remember { mutableStateOf<String?>(null) }

    // 编辑模式下加载已有数据
    LaunchedEffect(uiState.editingChannel) {
        uiState.editingChannel?.let { channel ->
            name = channel.name
            selectedProvider = channel.providerType
            baseUrl = channel.baseUrl
            selectedModelId = channel.defaultModelId
        }
    }

    LaunchedEffect(uiState.isChannelSaved) {
        if (uiState.isChannelSaved) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }

    // 自动填充 Base URL
    LaunchedEffect(selectedProvider) {
        if (!isEditMode && (baseUrl.isBlank() || isDefaultUrl(baseUrl))) {
            baseUrl = getDefaultUrl(selectedProvider)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑渠道" else "新建渠道") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 渠道名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("渠道名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Provider 选择
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    readOnly = true,
                    value = selectedProvider.value,
                    onValueChange = {},
                    label = { Text("提供商") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ProviderType.entries.filter { it != ProviderType.UNKNOWN }.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.value) },
                            onClick = {
                                selectedProvider = provider
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Base URL
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // API Key (仅新建模式显示)
            if (!isEditMode) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 流式开关
            StreamToggle(
                enabled = uiState.streamEnabled,
                onToggle = { viewModel.onEvent(ChannelEvent.ToggleStream(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 错误提示
            AnimatedVisibility(visible = uiState.error != null) {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 编辑模式下显示连接测试和模型列表
            if (isEditMode && uiState.editingChannel != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 连接测试
                ConnectionTestSection(
                    channelId = channelId!!,
                    isTesting = uiState.isTesting,
                    connectionStatus = uiState.connectionStatus,
                    onTestClick = { viewModel.onEvent(ChannelEvent.TestConnection(channelId)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 模型列表
                ModelListSection(
                    channelId = channelId,
                    models = uiState.models,
                    selectedModelId = selectedModelId,
                    isFetching = uiState.isFetchingModels,
                    error = uiState.fetchModelsError,
                    onRefresh = { viewModel.onEvent(ChannelEvent.FetchModels(channelId)) },
                    onSelectModel = { modelId ->
                        selectedModelId = modelId
                        viewModel.onEvent(ChannelEvent.SetDefaultModel(channelId, modelId))
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 保存按钮
            Button(
                onClick = {
                    if (isEditMode) {
                        viewModel.onEvent(
                            ChannelEvent.UpdateChannel(
                                name = name,
                                providerType = selectedProvider,
                                baseUrl = baseUrl,
                                defaultModelId = selectedModelId
                            )
                        )
                    } else {
                        viewModel.onEvent(
                            ChannelEvent.SaveChannel(
                                name = name,
                                providerType = selectedProvider,
                                baseUrl = baseUrl,
                                apiKey = apiKey
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && baseUrl.isNotBlank() &&
                        (isEditMode || apiKey.isNotBlank()) && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isEditMode) "保存修改" else "创建渠道")
                }
            }
        }
    }
}

@Composable
private fun ConnectionTestSection(
    channelId: String,
    isTesting: Boolean,
    connectionStatus: com.example.charisha.domain.repository.ConnectionStatus?,
    onTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "连接测试",
                style = MaterialTheme.typography.titleSmall
            )
            OutlinedButton(
                onClick = onTestClick,
                enabled = !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("测试中...")
                } else {
                    Text("测试连接")
                }
            }
        }

        // 连接状态显示
        AnimatedVisibility(visible = connectionStatus != null) {
            connectionStatus?.let { status ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.isConnected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (status.isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (status.isConnected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (status.isConnected) "连接成功" else "连接失败",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (status.isConnected && status.latencyMs != null) {
                                Text(
                                    text = "延迟: ${status.latencyMs}ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            status.errorMessage?.let { error ->
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelListSection(
    channelId: String,
    models: List<LlmModel>,
    selectedModelId: String?,
    isFetching: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onSelectModel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "模型列表 (${models.size})",
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(
                onClick = onRefresh,
                enabled = !isFetching
            ) {
                if (isFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新模型列表")
                }
            }
        }

        // 错误提示
        AnimatedVisibility(visible = error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (models.isEmpty() && !isFetching) {
            Text(
                text = "暂无模型，点击刷新按钮获取",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column {
                    models.forEachIndexed { index, model ->
                        ModelItem(
                            model = model,
                            isSelected = model.id == selectedModelId,
                            onClick = { onSelectModel(model.id) }
                        )
                        if (index < models.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelItem(
    model: LlmModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (model.supportsVision) {
                    ModelCapabilityChip("视觉")
                }
                if (model.supportsReasoning) {
                    ModelCapabilityChip("推理")
                }
                if (model.supportsImageGen) {
                    ModelCapabilityChip("生图")
                }
                if (!model.supportsStreaming) {
                    ModelCapabilityChip("非流式")
                }
            }
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ModelCapabilityChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(vertical = 2.dp)
    )
}

private fun isDefaultUrl(url: String): Boolean {
    return url.contains("api.openai.com") ||
            url.contains("generativelanguage.googleapis.com") ||
            url.contains("api.anthropic.com")
}

private fun getDefaultUrl(provider: ProviderType): String {
    return when (provider) {
        ProviderType.OPENAI -> "https://api.openai.com/v1"
        ProviderType.GEMINI -> "https://generativelanguage.googleapis.com/v1beta"
        ProviderType.CLAUDE -> "https://api.anthropic.com/v1"
        else -> ""
    }
}
