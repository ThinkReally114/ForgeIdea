package com.qz.agent.llm.model

import kotlinx.serialization.Serializable

@Serializable
data class StreamChunk(
    val content: String,
    val isDone: Boolean = false
)
