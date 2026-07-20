package com.qz.agent.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String = "opencode/big-pickle",
    val messageCount: Int = 0
)
