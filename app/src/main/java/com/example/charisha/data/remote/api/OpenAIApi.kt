package com.example.charisha.data.remote.api

import com.example.charisha.data.remote.dto.openai.OpenAIChatRequest
import com.example.charisha.data.remote.dto.openai.OpenAIChatResponse
import com.example.charisha.data.remote.dto.openai.OpenAIImageRequest
import com.example.charisha.data.remote.dto.openai.OpenAIImageResponse
import com.example.charisha.data.remote.dto.openai.OpenAIModelsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * OpenAI API 接口
 */
interface OpenAIApi {

    @GET("models")
    suspend fun listModels(
        @Header("Authorization") authorization: String
    ): Response<OpenAIModelsResponse>

    @GET
    suspend fun listModelsAt(
        @Header("Authorization") authorization: String,
        @Url url: String
    ): Response<OpenAIModelsResponse>

    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIChatRequest
    ): Response<OpenAIChatResponse>

    @POST("chat/completions")
    @Streaming
    suspend fun chatCompletionsStream(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIChatRequest
    ): Response<ResponseBody>

    @POST("images/generations")
    suspend fun generateImage(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIImageRequest
    ): Response<OpenAIImageResponse>

    companion object {
        fun buildAuthHeader(apiKey: String): String = "Bearer $apiKey"
    }
}
