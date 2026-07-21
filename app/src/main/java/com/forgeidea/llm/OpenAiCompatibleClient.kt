package com.forgeidea.llm

import android.util.Log
import com.forgeidea.domain.model.Provider
import com.forgeidea.llm.model.ChatMessage
import com.forgeidea.llm.model.ChatRequest
import com.forgeidea.llm.model.ChatResponse
import com.forgeidea.llm.model.StreamChunk
import com.forgeidea.llm.model.ToolDefinition
import com.forgeidea.llm.stream.SseParser
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class OpenAiCompatibleClient(
    private val httpClient: HttpClient,
    private val sseParser: SseParser = SseParser()
) : LlmClient {

    private val json = Json { ignoreUnknownKeys = true }

    override fun streamChat(
        messages: List<ChatMessage>,
        model: String,
        provider: Provider
    ): Flow<StreamChunk> = flow {
        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = true,
            temperature = 0.7
        )
        val url = "${provider.baseUrl.trimEnd('/')}/chat/completions"
        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
        Log.i(TAG, "streamChat: POST $url model=$model")

        val response: HttpResponse = httpClient.post(url) {
            header(HttpHeaders.Authorization, "Bearer ${provider.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
            header("Accept", "text/event-stream")
        }

        Log.i(TAG, "streamChat: response status=${response.status}")

        if (response.status.value >= 400) {
            val errBody = response.bodyAsChannel().readUTF8Line(limit = 8192) ?: ""
            throw IllegalStateException("API 返回 ${response.status.value}: $errBody")
        }

        val channel = response.bodyAsChannel()
        var hasContent = false
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line(limit = 65536) ?: break
            val chunk = sseParser.parse(line) ?: continue
            if (chunk.isDone) break
            if (chunk.content.isNotBlank() || chunk.reasoning.isNotBlank()) {
                hasContent = true
                emit(chunk)
            }
        }

        if (!hasContent) {
            throw IllegalStateException("API 返回内容为空")
        }
    }

    override suspend fun chat(
        messages: List<ChatMessage>,
        model: String,
        provider: Provider,
        tools: List<ToolDefinition>?
    ): ChatResponse {
        if (provider.apiKey.isBlank()) {
            throw IllegalStateException("API Key 未设置，请先到设置页配置")
        }

        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = false,
            temperature = 0.7,
            tools = tools
        )
        val url = "${provider.baseUrl.trimEnd('/')}/chat/completions"
        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
        Log.i(TAG, "chat: POST $url model=$model")

        val response: HttpResponse = httpClient.post(url) {
            header(HttpHeaders.Authorization, "Bearer ${provider.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val body = response.bodyAsText()
        Log.i(TAG, "chat: response status=${response.status} bodyLength=${body.length}")

        if (response.status.value >= 400) {
            throw IllegalStateException("API 返回 ${response.status.value}: ${body.take(500)}")
        }

        return json.decodeFromString(ChatResponse.serializer(), body)
    }

    companion object {
        private const val TAG = "OpenAiCompatibleClient"
    }
}
