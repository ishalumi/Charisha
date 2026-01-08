package com.example.charisha.data.remote.sse

import com.example.charisha.data.remote.dto.claude.ClaudeStreamEvent
import com.example.charisha.data.remote.dto.gemini.GeminiResponse
import com.example.charisha.data.remote.dto.openai.OpenAIChatResponse
import com.example.charisha.domain.model.StreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SSE 流式解析器
 */
@Singleton
class SSEParser @Inject constructor(
    private val json: Json
) {
    /**
     * 解析 OpenAI SSE 流
     * 格式: data: {json}\n\n
     * 终止: data: [DONE]
     */
    fun parseOpenAIStream(responseBody: ResponseBody): Flow<StreamEvent> = flow {
        responseBody.byteStream().bufferedReader().use { reader ->
            val dataBuffer = StringBuilder()
            reader.forEachLine { line ->
                when {
                    line.startsWith("data:") -> {
                        val data = line.removePrefix("data:").trim()
                        if (dataBuffer.isNotEmpty()) dataBuffer.append("\n")
                        dataBuffer.append(data)
                    }
                    line.isEmpty() && dataBuffer.isNotEmpty() -> {
                        val data = dataBuffer.toString()
                        dataBuffer.clear()
                        if (data == "[DONE]") {
                            emit(StreamEvent.Done)
                            return@forEachLine
                        }
                        try {
                            val response = json.decodeFromString<OpenAIChatResponse>(data)
                            response.choices.firstOrNull()?.let { choice ->
                                choice.delta?.let { delta ->
                                    delta.reasoningContent?.let { thinking ->
                                        emit(StreamEvent.ThinkingDelta(thinking))
                                    }
                                    delta.content?.let { content ->
                                        emit(StreamEvent.TextDelta(content))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            emit(StreamEvent.Error("PARSE_ERROR", e.message ?: "Failed to parse OpenAI response"))
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 解析 Claude SSE 流
     * 格式: event: {type}\ndata: {json}\n\n
     * 终止: event: message_stop
     */
    fun parseClaudeStream(responseBody: ResponseBody): Flow<StreamEvent> = flow {
        responseBody.byteStream().bufferedReader().use { reader ->
            var currentEvent: String? = null

            reader.forEachLine { line ->
                when {
                    line.startsWith("event:") -> {
                        currentEvent = line.removePrefix("event:").trim()
                    }
                    line.startsWith("data:") -> {
                        val data = line.removePrefix("data:").trim()
                        if (data.isNotEmpty()) {
                            try {
                                val event = json.decodeFromString<ClaudeStreamEvent>(data)
                                when (event.type) {
                                    "content_block_delta" -> {
                                        event.delta?.let { delta ->
                                            delta.thinking?.let { emit(StreamEvent.ThinkingDelta(it)) }
                                            delta.text?.let { emit(StreamEvent.TextDelta(it)) }
                                        }
                                    }
                                    "message_stop" -> emit(StreamEvent.Done)
                                    "error" -> {
                                        event.error?.let {
                                            emit(StreamEvent.Error(it.type, it.message))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                emit(StreamEvent.Error("PARSE_ERROR", e.message ?: "Failed to parse Claude response"))
                            }
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 解析 Gemini NDJSON 流
     * 格式: {json}\n{json}\n...
     */
    fun parseGeminiNDJSONStream(responseBody: ResponseBody): Flow<StreamEvent> = flow {
        responseBody.byteStream().bufferedReader().use { reader ->
            reader.forEachLine { line ->
                if (line.isNotBlank()) {
                    try {
                        val response = json.decodeFromString<GeminiResponse>(line)
                        response.candidates?.firstOrNull()?.content?.parts?.forEach { part ->
                            part.thought?.let { emit(StreamEvent.ThinkingDelta(it)) }
                            part.text?.let { emit(StreamEvent.TextDelta(it)) }
                        }
                        response.error?.let {
                            emit(StreamEvent.Error(it.status ?: "ERROR", it.message ?: "Unknown error"))
                        }
                    } catch (e: Exception) {
                        emit(StreamEvent.Error("PARSE_ERROR", e.message ?: "Failed to parse Gemini response"))
                    }
                }
            }
            emit(StreamEvent.Done)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 解析 Gemini SSE 流 (使用 ?alt=sse 参数时)
     */
    fun parseGeminiSSEStream(responseBody: ResponseBody): Flow<StreamEvent> = flow {
        responseBody.byteStream().bufferedReader().use { reader ->
            val dataBuffer = StringBuilder()
            reader.forEachLine { line ->
                when {
                    line.startsWith("data:") -> {
                        val data = line.removePrefix("data:").trim()
                        if (dataBuffer.isNotEmpty()) dataBuffer.append("\n")
                        dataBuffer.append(data)
                    }
                    line.isEmpty() && dataBuffer.isNotEmpty() -> {
                        val data = dataBuffer.toString()
                        dataBuffer.clear()
                        try {
                            val response = json.decodeFromString<GeminiResponse>(data)
                            response.candidates?.firstOrNull()?.content?.parts?.forEach { part ->
                                part.thought?.let { emit(StreamEvent.ThinkingDelta(it)) }
                                part.text?.let { emit(StreamEvent.TextDelta(it)) }
                            }
                            response.error?.let {
                                emit(StreamEvent.Error(it.status ?: "ERROR", it.message ?: "Unknown error"))
                            }
                        } catch (e: Exception) {
                            emit(StreamEvent.Error("PARSE_ERROR", e.message ?: "Failed to parse Gemini SSE response"))
                        }
                    }
                }
            }
            emit(StreamEvent.Done)
        }
    }.flowOn(Dispatchers.IO)
}
