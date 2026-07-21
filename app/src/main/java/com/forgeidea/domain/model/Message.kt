package com.forgeidea.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val sessionId: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null
)

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String
)
