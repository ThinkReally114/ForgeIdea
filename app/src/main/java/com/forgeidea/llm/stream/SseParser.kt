package com.forgeidea.llm.stream

import android.util.Log
import com.forgeidea.llm.model.StreamChunk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(line: String): StreamChunk? {
        if (line.isBlank()) return null
        if (!line.startsWith("data:")) {
            Log.d(TAG, "not SSE data line: $line")
            return null
        }
        val data = line.removePrefix("data:").trim()
        if (data == "[DONE]") return StreamChunk("", isDone = true)
        return try {
            val obj = json.parseToJsonElement(data).jsonObject
            val choices = obj["choices"]?.jsonArray
            if (choices == null || choices.isEmpty()) {
                Log.d(TAG, "no choices in: $data")
                return null
            }
            val deltaObj = choices[0].jsonObject["delta"]?.jsonObject
            if (deltaObj == null) {
                // 可能是非流式响应结构：choices[0].message.content
                val message = choices[0].jsonObject["message"]?.jsonObject
                val content = message?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
                if (content.isNotBlank()) {
                    Log.d(TAG, "parsed non-stream message: $content")
                    return StreamChunk(content)
                }
                Log.d(TAG, "no delta/message in: $data")
                return null
            }
            val content = deltaObj["content"]?.jsonPrimitive?.contentOrNull ?: ""
            val reasoning = deltaObj["reasoning_content"]?.jsonPrimitive?.contentOrNull
                ?: deltaObj["reasoning"]?.jsonPrimitive?.contentOrNull
                ?: ""
            Log.d(TAG, "parsed delta content='$content' reasoning='$reasoning'")
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
