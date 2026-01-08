package com.example.charisha.data.remote.api

import com.example.charisha.data.remote.dto.gemini.GeminiImageRequest
import com.example.charisha.data.remote.dto.gemini.GeminiImageResponse
import com.example.charisha.data.remote.dto.gemini.GeminiModelsResponse
import com.example.charisha.data.remote.dto.gemini.GeminiRequest
import com.example.charisha.data.remote.dto.gemini.GeminiResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Gemini API 接口 (原生 API)
 */
interface GeminiApi {

    @GET("models")
    suspend fun listModels(
        @Query("key") apiKey: String
    ): Response<GeminiModelsResponse>

    @POST("models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>

    @POST("models/{model}:streamGenerateContent")
    @Streaming
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<ResponseBody>

    @POST("models/{model}:streamGenerateContent")
    @Streaming
    suspend fun streamGenerateContentSSE(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Query("alt") alt: String = "sse",
        @Body request: GeminiRequest
    ): Response<ResponseBody>

    @POST("models/{model}:predict")
    suspend fun generateImage(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiImageRequest
    ): Response<GeminiImageResponse>
}
