package com.qz.agent.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole {
    SYSTEM, USER, ASSISTANT, TOOL
}
