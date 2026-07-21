package com.forgeidea.domain.usecase

import android.util.Log
import com.forgeidea.domain.model.Message
import com.forgeidea.domain.model.Provider
import com.forgeidea.llm.LlmClient
import com.forgeidea.llm.model.ChatMessage
import com.forgeidea.llm.model.ResponseMessage
import com.forgeidea.llm.model.ToolDefinition
import com.forgeidea.tools.WorkspaceFileTools

class SendMessageUseCase(
    private val llmClient: LlmClient,
    private val workspaceFileTools: WorkspaceFileTools
) {

    suspend fun send(
        sessionId: String,
        history: List<Message>,
        userInput: String,
        model: String,
        provider: Provider
    ): Result {
        val chatMessages = history.map { it.toChatMessage() }.toMutableList()
        chatMessages.add(ChatMessage(role = "user", content = userInput))
        val tools = workspaceFileTools.buildToolDefinitions()

        val startTime = System.currentTimeMillis()
        var finalContent = ""
        var finalReasoning = ""
        val maxIterations = 5

        repeat(maxIterations) { iteration ->
            Log.i(TAG, "send iteration=$iteration messages=${chatMessages.size}")
            val response = llmClient.chat(chatMessages, model, provider, tools)
            val message = response.choices.firstOrNull()?.message
                ?: throw IllegalStateException("API 返回空 choices")

            if (message.toolCalls?.isNotEmpty() == true) {
                chatMessages.add(message.toChatMessage())
                message.toolCalls.forEach { toolCall ->
                    val result = workspaceFileTools.executeTool(
                        sessionId = sessionId,
                        name = toolCall.function.name,
                        arguments = toolCall.function.arguments
                    )
                    chatMessages.add(
                        ChatMessage(
                            role = "tool",
                            content = result,
                            toolCallId = toolCall.id
                        )
                    )
                }
            } else {
                finalContent = message.content ?: ""
                finalReasoning = message.reasoningContent ?: message.reasoning ?: ""
                val durationMs = System.currentTimeMillis() - startTime
                return Result(
                    content = finalContent,
                    reasoning = finalReasoning,
                    durationMs = durationMs
                )
            }
        }

        val durationMs = System.currentTimeMillis() - startTime
        return Result(
            content = finalContent,
            reasoning = finalReasoning,
            durationMs = durationMs
        )
    }

    private fun Message.toChatMessage(): ChatMessage {
        return when (role.name.uppercase()) {
            "USER" -> ChatMessage(role = "user", content = content)
            "ASSISTANT" -> ChatMessage(role = "assistant", content = content)
            "TOOL" -> ChatMessage(role = "tool", content = content, toolCallId = toolCallId)
            else -> ChatMessage(role = "system", content = content)
        }
    }

    private fun ResponseMessage.toChatMessage(): ChatMessage {
        return ChatMessage(
            role = role,
            content = content,
            toolCalls = toolCalls
        )
    }

    data class Result(
        val content: String,
        val reasoning: String,
        val durationMs: Long
    )

    companion object {
        private const val TAG = "SendMessageUseCase"
    }
}
