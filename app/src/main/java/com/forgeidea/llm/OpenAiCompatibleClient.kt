package com.forgeidea.llm

import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.llm.model.ChatMessage
import com.forgeidea.llm.model.ChatRequest
import com.forgeidea.llm.model.ChatResponse
import com.forgeidea.llm.model.StreamChunk
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
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class OpenAiCompatibleClient(
    private val httpClient: HttpClient,
    private val keyStore: ApiKeyStore,
    private val sseParser: SseParser = SseParser()
) : LlmClient {

    private val json = Json { ignoreUnknownKeys = true }

    override fun streamChat(messages: List<ChatMessage>, model: String): Flow<StreamChunk> = flow {
        val apiKey = keyStore.getApiKey() ?: ""
        val baseUrl = keyStore.getBaseUrl() ?: "https://api.opencode.ai/v1"

        if (apiKey.isBlank()) {
            throw IllegalStateException("API Key 未设置，请先到设置页配置")
        }

        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = false,
            temperature = 0.7
        )
        val url = "${baseUrl.trimEnd('/')}/chat/completions"
        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
        Log.i(TAG, "chat: POST $url model=$model body=$requestBody")

        val response: HttpResponse = httpClient.post(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        Log.i(TAG, "chat: response status=${response.status}")

        if (response.status.value >= 400) {
            val errBody = response.bodyAsChannel().readUTF8Line(limit = 8192) ?: ""
            throw IllegalStateException("API 返回 ${response.status.value}: $errBody")
        }

        val body = response.bodyAsText()
        Log.i(TAG, "chat: response body=$body")

        val chatResponse = try {
            json.decodeFromString(ChatResponse.serializer(), body)
        } catch (e: Exception) {
            throw IllegalStateException("解析响应失败: ${e.message}")
        }

        val message = chatResponse.choices.firstOrNull()?.message
            ?: throw IllegalStateException("API 返回 choices 为空")

        val content = message.content ?: ""
        val reasoning = message.reasoningContent ?: message.reasoning ?: ""

        if (content.isBlank() && reasoning.isBlank()) {
            throw IllegalStateException("API 返回内容为空")
        }

        emit(StreamChunk(content = content, reasoning = reasoning))
    }

    override suspend fun chat(messages: List<ChatMessage>, model: String): String {
        val builder = StringBuilder()
        streamChat(messages, model).collect { chunk ->
            builder.append(chunk.content)
        }
        return builder.toString()
    }

    companion object {
        private const val TAG = "OpenAiCompatibleClient"
    }
}
