package com.example.charisha.domain.usecase.channel

import com.example.charisha.domain.repository.ChannelRepository
import com.example.charisha.domain.repository.ConnectionStatus
import javax.inject.Inject

/**
 * 测试渠道连接用例
 */
class TestConnectionUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    suspend operator fun invoke(channelId: String): Result<ConnectionStatus> {
        return channelRepository.testConnection(channelId)
    }
}
