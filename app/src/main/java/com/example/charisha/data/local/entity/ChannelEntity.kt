package com.example.charisha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 渠道配置实体
 */
@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val providerType: String,
    val baseUrl: String,
    val apiKeyRef: String,
    val customHeadersJson: String?,
    val proxyType: String,
    val proxyHost: String?,
    val proxyPort: Int?,
    val defaultModelId: String?,
    val streamEnabled: Boolean,
    val imageGenModelId: String?,
    val modelsApiPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)
