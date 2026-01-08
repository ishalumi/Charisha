package com.example.charisha.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安全存储 - 使用 EncryptedSharedPreferences 加密存储敏感数据
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * 保存 API Key
     * @param keyRef 密钥引用标识 (对应 ChannelEntity.apiKeyRef)
     * @param apiKey API 密钥明文
     */
    fun saveApiKey(keyRef: String, apiKey: String) {
        prefs.edit().putString(buildApiKeyKey(keyRef), apiKey).apply()
    }

    /**
     * 获取 API Key
     * @param keyRef 密钥引用标识
     * @return API 密钥明文，不存在则返回 null
     */
    fun getApiKey(keyRef: String): String? {
        return prefs.getString(buildApiKeyKey(keyRef), null)
    }

    /**
     * 删除 API Key
     * @param keyRef 密钥引用标识
     */
    fun deleteApiKey(keyRef: String) {
        prefs.edit().remove(buildApiKeyKey(keyRef)).apply()
    }

    /**
     * 检查 API Key 是否存在
     * @param keyRef 密钥引用标识
     */
    fun hasApiKey(keyRef: String): Boolean {
        return prefs.contains(buildApiKeyKey(keyRef))
    }

    /**
     * 清除所有存储的 API Key
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun buildApiKeyKey(keyRef: String): String = "$KEY_PREFIX_API_KEY$keyRef"

    companion object {
        private const val PREFS_NAME = "charisha_secure_prefs"
        private const val KEY_PREFIX_API_KEY = "api_key_"
    }
}
