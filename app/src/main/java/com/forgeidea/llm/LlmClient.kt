package com.forgeidea.llm

import com.forgeidea.llm.model.StreamChunk
import kotlinx.coroutines.flow.Flow

interface LlmClient {
    fun streamChat(messages: List<com.forgeidea.llm.model.ChatMessage>, model: String): Flow<StreamChunk>
    suspend fun chat(messages: List<com.forgeidea.llm.model.ChatMessage>, model: String): String
}
