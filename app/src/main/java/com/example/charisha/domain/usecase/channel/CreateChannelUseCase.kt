package com.example.charisha.domain.usecase.channel

import com.example.charisha.data.local.prefs.SecurePreferences
import com.example.charisha.domain.model.Channel
import com.example.charisha.domain.model.ProviderType
import com.example.charisha.domain.model.ProxyType
import com.example.charisha.domain.repository.ChannelRepository
import java.util.UUID
import javax.inject.Inject

/**
 * 创建渠道用例
 */
class CreateChannelUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val securePreferences: SecurePreferences
) {
    suspend operator fun invoke(params: Params): Result<Channel> = runCatching {
        val now = System.currentTimeMillis()
        val channelId = UUID.randomUUID().toString()

        securePreferences.saveApiKey(channelId, params.apiKey)

        val channel = Channel(
            id = channelId,
            name = params.name,
            providerType = params.providerType,
            baseUrl = params.baseUrl.trimEnd('/'),
            apiKeyRef = channelId,
            customHeaders = params.customHeaders,
            proxyType = params.proxyType,
            proxyHost = params.proxyHost,
            proxyPort = params.proxyPort,
            defaultModelId = null,
            streamEnabled = params.streamEnabled,
            imageGenModelId = null,
            modelsApiPath = params.modelsApiPath,
            createdAt = now,
            updatedAt = now
        )

        channelRepository.createChannel(channel).getOrThrow()
        channel
    }

    data class Params(
        val name: String,
        val providerType: ProviderType,
        val baseUrl: String,
        val apiKey: String,
        val customHeaders: Map<String, String>? = null,
        val proxyType: ProxyType = ProxyType.NONE,
        val proxyHost: String? = null,
        val proxyPort: Int? = null,
        val streamEnabled: Boolean = true,
        val modelsApiPath: String? = null
    )
}
