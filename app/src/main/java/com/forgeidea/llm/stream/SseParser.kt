package com.forgeidea.llm.stream

import android.util.Log
import com.forgeidea.llm.model.StreamChunk
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
            val delta = choices[0].jsonObject["delta"]?.jsonObject ?: return null
            val content = delta["content"]?.jsonPrimitive?.contentOrNull ?: ""
            val reasoning = delta["reasoning_content"]?.jsonPrimitive?.contentOrNull
                ?: delta["reasoning"]?.jsonPrimitive?.contentOrNull
                ?: ""
            StreamChunk(content = content, reasoning = reasoning)
        } catch (e: Exception) {
            Log.w(TAG, "parse failed: ${e.message} line=$line")
            null
        }
    }

    companion object {
        private const val TAG = "SseParser"
    }
}
