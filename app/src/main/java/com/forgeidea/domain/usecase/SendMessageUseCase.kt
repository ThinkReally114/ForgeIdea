package com.forgeidea.domain.usecase

import com.forgeidea.llm.LlmClient
import com.forgeidea.llm.model.ChatMessage
import com.forgeidea.llm.model.StreamChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SendMessageUseCase(
    private val llmClient: LlmClient
) {
    operator fun invoke(history: List<com.forgeidea.domain.model.Message>, userInput: String): Flow<StreamChunk> {
        val chatMessages = history.map { msg ->
            ChatMessage(role = msg.role.name.lowercase(), content = msg.content)
        } + ChatMessage(role = "user", content = userInput)
        return llmClient.streamChat(chatMessages)
    }
}
