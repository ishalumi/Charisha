package com.example.charisha.data.provider

import com.example.charisha.data.remote.api.ClaudeApi
import com.example.charisha.data.remote.api.GeminiApi
import com.example.charisha.data.remote.api.OpenAIApi
import com.example.charisha.data.remote.ApiUrl
import com.example.charisha.data.remote.sse.SSEParser
import com.example.charisha.domain.model.ProviderType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderFactory @Inject constructor(
    private val retrofitBuilder: Retrofit.Builder,
    private val okHttpClient: OkHttpClient,
    private val sseParser: SSEParser
) {

    fun create(providerType: ProviderType, baseUrl: String): LLMProvider {
        val normalizedBaseUrl = ApiUrl.normalizeProviderBaseUrl(providerType, baseUrl)
        val retrofit = retrofitBuilder.baseUrl(normalizedBaseUrl).build()
        val resolver = BinaryResolver(okHttpClient)

        return when (providerType) {
            ProviderType.OPENAI -> OpenAIProvider(
                api = retrofit.create(OpenAIApi::class.java),
                sseParser = sseParser,
                binaryResolver = resolver
            )
            ProviderType.GEMINI -> GeminiProvider(
                api = retrofit.create(GeminiApi::class.java),
                sseParser = sseParser,
                binaryResolver = resolver
            )
            ProviderType.CLAUDE -> ClaudeProvider(
                api = retrofit.create(ClaudeApi::class.java),
                sseParser = sseParser,
                binaryResolver = resolver
            )
            ProviderType.UNKNOWN -> throw IllegalArgumentException("未知 Provider")
        }
    }
}
