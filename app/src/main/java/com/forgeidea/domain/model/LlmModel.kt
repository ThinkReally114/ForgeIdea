package com.forgeidea.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LlmModel(
    val id: String,
    val name: String = id
)
