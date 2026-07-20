package com.qz.agent.llm

import com.qz.agent.llm.model.ChatMessage
import com.qz.agent.llm.model.ChatRequest
import com.qz.agent.llm.model.StreamChunk
import com.qz.agent.llm.stream.SseParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class OpenAiCompatibleClient(
    private val httpClient: HttpClient,
    private val config: ZenConfig,
    private val sseParser: SseParser = SseParser()
) : LlmClient {

    private val json = Json { ignoreUnknownKeys = true }

    override fun streamChat(messages: List<ChatMessage>): Flow<StreamChunk> = flow {
        val request = ChatRequest(
            model = config.model,
            messages = messages,
            stream = true,
            temperature = config.temperature
        )
        val response: HttpResponse = httpClient.post("${config.baseUrl}/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatRequest.serializer(), request))
        }

        val channel = response.bodyAsChannel()
        val buffer = StringBuilder()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            val chunk = sseParser.parse(line)
            if (chunk != null) {
                if (chunk.isDone) break
                if (chunk.content.isNotEmpty()) emit(chunk)
            }
        }
    }

    override suspend fun chat(messages: List<ChatMessage>): String {
        val builder = StringBuilder()
        streamChat(messages).collect { chunk ->
            builder.append(chunk.content)
        }
        return builder.toString()
    }
}
