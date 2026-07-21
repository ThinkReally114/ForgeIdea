package com.forgeidea.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole {
    SYSTEM, USER, ASSISTANT, TOOL
}
