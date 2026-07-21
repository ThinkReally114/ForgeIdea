package com.forgeidea.llm

import com.forgeidea.domain.model.Provider
import com.forgeidea.llm.model.ChatMessage
import com.forgeidea.llm.model.ChatResponse
import com.forgeidea.llm.model.StreamChunk
import com.forgeidea.llm.model.ToolDefinition
import kotlinx.coroutines.flow.Flow

interface LlmClient {
    fun streamChat(
        messages: List<ChatMessage>,
        model: String,
        provider: Provider
    ): Flow<StreamChunk>

    suspend fun chat(
        messages: List<ChatMessage>,
        model: String,
        provider: Provider,
        tools: List<ToolDefinition>? = null
    ): ChatResponse
}