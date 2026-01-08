package com.example.charisha.data.local.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.example.charisha.domain.model.ContentPart
import com.example.charisha.domain.model.MessageRole
import com.example.charisha.domain.model.ProviderType
import com.example.charisha.domain.model.ProxyType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Room 类型转换器
 */
@ProvidedTypeConverter
class Converters @Inject constructor(
    private val json: Json
) {
    // ProviderType
    @TypeConverter
    fun providerTypeToString(value: ProviderType): String = value.value

    @TypeConverter
    fun stringToProviderType(value: String): ProviderType = ProviderType.fromValue(value)

    // ProxyType
    @TypeConverter
    fun proxyTypeToString(value: ProxyType): String = value.value

    @TypeConverter
    fun stringToProxyType(value: String): ProxyType = ProxyType.fromValue(value)

    // MessageRole
    @TypeConverter
    fun messageRoleToString(value: MessageRole): String = value.value

    @TypeConverter
    fun stringToMessageRole(value: String): MessageRole = MessageRole.fromValue(value)

    // ContentPart List
    @TypeConverter
    fun contentPartsToJson(value: List<ContentPart>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToContentParts(value: String): List<ContentPart> = json.decodeFromString(value)

    // Map<String, String> for custom headers
    @TypeConverter
    fun mapToJson(value: Map<String, String>?): String? =
        value?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToMap(value: String?): Map<String, String>? =
        value?.let { json.decodeFromString(it) }
}
