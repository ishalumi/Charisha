package com.example.charisha.data.remote.api

import com.example.charisha.data.remote.dto.claude.ClaudeModelsResponse
import com.example.charisha.data.remote.dto.claude.ClaudeRequest
import com.example.charisha.data.remote.dto.claude.ClaudeResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Claude API 接口
 */
interface ClaudeApi {

    @GET("models")
    suspend fun listModels(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = ANTHROPIC_VERSION,
        @Query("limit") limit: Int = 100,
        @Query("after_id") afterId: String? = null
    ): Response<ClaudeModelsResponse>

    @POST("messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = ANTHROPIC_VERSION,
        @Body request: ClaudeRequest
    ): Response<ClaudeResponse>

    @POST("messages")
    @Streaming
    suspend fun createMessageStream(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = ANTHROPIC_VERSION,
        @Body request: ClaudeRequest
    ): Response<ResponseBody>

    companion object {
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
