package com.example.charisha.data.local.mapper

import com.example.charisha.data.local.entity.ChannelEntity
import com.example.charisha.data.local.entity.ConversationEntity
import com.example.charisha.data.local.entity.MessageEntity
import com.example.charisha.data.local.entity.ModelEntity
import com.example.charisha.domain.model.Channel
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.Conversation
import com.example.charisha.domain.model.LlmModel
import com.example.charisha.domain.model.Message
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.ProviderType
import com.example.charisha.domain.model.ProxyType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Entity <-> Domain Model 映射器
 */
object EntityMapper {

    fun ChannelEntity.toDomain(customHeaders: Map<String, String>?): Channel = Channel(
        id = id,
        name = name,
        providerType = ProviderType.fromValue(providerType),
        baseUrl = baseUrl,
        apiKeyRef = apiKeyRef,
        customHeaders = customHeaders,
        proxyType = ProxyType.fromValue(proxyType),
        proxyHost = proxyHost,
        proxyPort = proxyPort,
        defaultModelId = defaultModelId,
        streamEnabled = streamEnabled,
        imageGenModelId = imageGenModelId,
        modelsApiPath = modelsApiPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun Channel.toEntity(json: Json): ChannelEntity = ChannelEntity(
        id = id,
        name = name,
        providerType = providerType.value,
        baseUrl = baseUrl,
        apiKeyRef = apiKeyRef,
        customHeadersJson = customHeaders?.let { json.encodeToString(it) },
        proxyType = proxyType.value,
        proxyHost = proxyHost,
        proxyPort = proxyPort,
        defaultModelId = defaultModelId,
        streamEnabled = streamEnabled,
        imageGenModelId = imageGenModelId,
        modelsApiPath = modelsApiPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun ModelEntity.toDomain(): LlmModel = LlmModel(
        id = id,
        channelId = channelId,
        displayName = displayName,
        contextLength = contextLength,
        maxOutputTokens = maxOutputTokens,
        supportsVision = supportsVision,
        supportsImageGen = supportsImageGen,
        supportsReasoning = supportsReasoning,
        supportsStreaming = supportsStreaming,
        defaultTemperature = defaultTemperature,
        defaultTopP = defaultTopP,
        isEnabled = isEnabled,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun LlmModel.toEntity(): ModelEntity = ModelEntity(
        id = id,
        channelId = channelId,
        displayName = displayName,
        contextLength = contextLength,
        maxOutputTokens = maxOutputTokens,
        supportsVision = supportsVision,
        supportsImageGen = supportsImageGen,
        supportsReasoning = supportsReasoning,
        supportsStreaming = supportsStreaming,
        defaultTemperature = defaultTemperature,
        defaultTopP = defaultTopP,
        isEnabled = isEnabled,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun ConversationEntity.toDomain(): Conversation = Conversation(
        id = id,
        channelId = channelId,
        modelId = modelId,
        title = title,
        systemPrompt = systemPrompt,
        parentMessageId = parentMessageId,
        rootConversationId = rootConversationId,
        lastMessageTime = lastMessageTime,
        createdAt = createdAt
    )

    fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
        id = id,
        channelId = channelId,
        modelId = modelId,
        title = title,
        systemPrompt = systemPrompt,
        parentMessageId = parentMessageId,
        rootConversationId = rootConversationId,
        lastMessageTime = lastMessageTime,
        createdAt = createdAt
    )

    fun MessageEntity.toDomain(content: List<ContentPart>): Message = Message(
        id = id,
        conversationId = conversationId,
        parentId = parentId,
        role = MessageRole.fromValue(role),
        content = content,
        thinking = thinkingJson,
        thinkingCollapsed = thinkingCollapsed,
        modelUsed = modelUsed,
        tokenCount = tokenCount,
        isEdited = isEdited,
        editedAt = editedAt,
        createdAt = createdAt
    )

    fun Message.toEntity(json: Json): MessageEntity = MessageEntity(
        id = id,
        conversationId = conversationId,
        parentId = parentId,
        role = role.value,
        contentJson = json.encodeToString(content),
        thinkingJson = thinking,
        thinkingCollapsed = thinkingCollapsed,
        modelUsed = modelUsed,
        tokenCount = tokenCount,
        isEdited = isEdited,
        editedAt = editedAt,
        createdAt = createdAt
    )
}
