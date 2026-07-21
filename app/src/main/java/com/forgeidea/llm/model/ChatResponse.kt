package com.forgeidea.llm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val index: Int,
    val message: ResponseMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ResponseMessage(
    val role: String,
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    val reasoning: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallResponse>? = null
)

@Serializable
data class ToolCallResponse(
    val id: String,
    val type: String,
    val function: ToolFunctionCall
)

@Serializable
data class ToolFunctionCall(
    val name: String,
    val arguments: String
)
