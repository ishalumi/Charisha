package com.example.charisha.data.repository

import com.example.charisha.data.local.dao.ChannelDao
import com.example.charisha.data.local.dao.ConversationDao
import com.example.charisha.data.local.dao.MessageDao
import com.example.charisha.data.local.dao.ModelDao
import com.example.charisha.data.local.prefs.SecurePreferences
import com.example.charisha.data.provider.LLMProvider
import com.example.charisha.data.provider.ProviderFactory
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.ProviderType
import com.example.charisha.domain.model.StreamEvent
import com.example.charisha.domain.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val modelDao: ModelDao,
    private val securePreferences: SecurePreferences,
    private val json: Json,
    private val providerFactory: ProviderFactory
) : ChatRepository {

    private val activeJobs = ConcurrentHashMap<String, Job>()

    override fun sendMessage(
        conversationId: String,
        content: List<ContentPart>,
        streamEnabled: Boolean
    ): Flow<StreamEvent> = flow {
        currentCoroutineContext()[Job]?.let { job ->
            activeJobs[conversationId] = job
        }

        val conversation = conversationDao.getById(conversationId)
            ?: throw IllegalArgumentException("对话不存在: $conversationId")

        val channel = channelDao.getById(conversation.channelId)
            ?: throw IllegalArgumentException("渠道不存在: ${conversation.channelId}")

        val modelId = conversation.modelId ?: channel.defaultModelId
            ?: throw IllegalArgumentException("未指定模型")

        val model = modelDao.getById(modelId, channel.id)

        val apiKey = securePreferences.getApiKey(channel.apiKeyRef)
            ?: throw IllegalStateException("API Key 未配置")

        val historyEntities = messageDao.getByConversationSync(conversationId)
        val fullHistory = historyEntities.map { entity ->
            val contentParts = json.decodeFromString<List<ContentPart>>(entity.contentJson)
            Message(
                id = entity.id,
                conversationId = entity.conversationId,
                parentId = entity.parentId,
                role = MessageRole.fromValue(entity.role),
                content = contentParts,
                thinking = entity.thinkingJson,
                thinkingCollapsed = entity.thinkingCollapsed,
                modelUsed = entity.modelUsed,
                tokenCount = entity.tokenCount,
                isEdited = entity.isEdited,
                editedAt = entity.editedAt,
                createdAt = entity.createdAt
            )
        }

        // SendMessageUseCase/EditMessageUseCase 会先把用户消息写入数据库，再调用 sendMessage(newContent)。
        // 这里需要避免“历史里已包含该用户消息 + newContent 又追加一次”的重复发送。
        val history = if (content.isNotEmpty() && fullHistory.isNotEmpty()) {
            val last = fullHistory.last()
            if (last.role == MessageRole.USER && last.content == content) {
                fullHistory.dropLast(1)
            } else {
                fullHistory
            }
        } else {
            fullHistory
        }

        val providerType = ProviderType.fromValue(channel.providerType)
        val useStream = streamEnabled && (model?.supportsStreaming ?: true)
        val maxTokens = model?.maxOutputTokens ?: 4096
        val temperature = model?.defaultTemperature ?: 0.7f

        val provider = providerFactory.create(providerType, channel.baseUrl)
        provider.sendMessage(
            LLMProvider.SendParams(
                modelId = modelId,
                apiKey = apiKey,
                systemPrompt = conversation.systemPrompt,
                history = history,
                newContent = content,
                useStream = useStream,
                maxTokens = maxTokens,
                temperature = temperature
            )
        ).collect { emit(it) }
    }.flowOn(Dispatchers.IO)
        .onCompletion { activeJobs.remove(conversationId) }

    override fun regenerateResponse(messageId: String): Flow<StreamEvent> = flow {
        val message = messageDao.getById(messageId)
            ?: throw IllegalArgumentException("消息不存在: $messageId")

        if (MessageRole.fromValue(message.role) != MessageRole.ASSISTANT) {
            throw IllegalArgumentException("只能重新生成 AI 消息")
        }

        messageDao.deleteAfter(message.conversationId, message.createdAt)
        messageDao.deleteById(messageId)

        val conversation = conversationDao.getById(message.conversationId)
            ?: throw IllegalArgumentException("对话不存在")

        val channel = channelDao.getById(conversation.channelId)
            ?: throw IllegalArgumentException("渠道不存在")

        sendMessage(
            conversationId = message.conversationId,
            content = emptyList(),
            streamEnabled = channel.streamEnabled
        ).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancelGeneration(conversationId: String) {
        activeJobs[conversationId]?.cancel(CancellationException("用户取消"))
        activeJobs.remove(conversationId)
    }

    override suspend fun generateImage(
        conversationId: String,
        prompt: String,
        size: String,
        n: Int
    ): Result<StreamEvent.ImageGenerated> = runCatching {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) throw IllegalArgumentException("提示词不能为空")

        val conversation = conversationDao.getById(conversationId)
            ?: throw IllegalArgumentException("对话不存在: $conversationId")

        val channel = channelDao.getById(conversation.channelId)
            ?: throw IllegalArgumentException("渠道不存在: ${conversation.channelId}")

        val providerType = ProviderType.fromValue(channel.providerType)
        val modelId = channel.imageGenModelId ?: conversation.modelId ?: channel.defaultModelId
            ?: throw IllegalArgumentException("未指定图片生成模型")

        val apiKey = securePreferences.getApiKey(channel.apiKeyRef)
            ?: throw IllegalStateException("API Key 未配置")

        val provider = providerFactory.create(providerType, channel.baseUrl)
        if (!provider.capabilities.supportsImageGeneration) {
            throw UnsupportedOperationException("当前提供商不支持图片生成")
        }

        provider.generateImage(
            LLMProvider.ImageGenParams(
                modelId = modelId,
                apiKey = apiKey,
                prompt = trimmed,
                size = size,
                n = n
            )
        ).getOrThrow()
    }
}
