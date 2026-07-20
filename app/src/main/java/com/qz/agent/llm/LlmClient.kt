package com.qz.agent.llm

import com.qz.agent.llm.model.StreamChunk
import kotlinx.coroutines.flow.Flow

interface LlmClient {
    fun streamChat(messages: List<com.qz.agent.llm.model.ChatMessage>): Flow<StreamChunk>
    suspend fun chat(messages: List<com.qz.agent.llm.model.ChatMessage>): String
}
