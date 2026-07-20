package com.qz.agent.llm.stream

import com.qz.agent.llm.model.StreamChunk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(line: String): StreamChunk? {
        if (line.isBlank()) return null
        if (!line.startsWith("data:")) return null
        val data = line.removePrefix("data:").trim()
        if (data == "[DONE]") return StreamChunk("", isDone = true)
        return try {
            val obj = json.parseToJsonElement(data).jsonObject
            val choices = obj["choices"]?.jsonArray ?: return null
            if (choices.isEmpty()) return null
            val delta = choices[0].jsonObject["delta"]?.jsonObject
            val content = delta?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
            StreamChunk(content)
        } catch (e: Exception) {
            null
        }
    }
}
