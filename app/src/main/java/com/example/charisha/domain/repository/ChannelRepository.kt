package com.example.charisha.domain.repository

import com.example.charisha.domain.model.Channel
import kotlinx.coroutines.flow.Flow

/**
 * 渠道仓库接口
 */
interface ChannelRepository {
    fun observeChannels(): Flow<List<Channel>>
    fun observeChannelById(id: String): Flow<Channel?>
    suspend fun createChannel(channel: Channel): Result<Unit>
    suspend fun updateChannel(channel: Channel): Result<Unit>
    suspend fun deleteChannel(id: String): Result<Unit>
    suspend fun testConnection(channelId: String): Result<ConnectionStatus>
}

data class ConnectionStatus(
    val isConnected: Boolean,
    val latencyMs: Long? = null,
    val errorMessage: String? = null
)
