package com.qz.agent.llm

data class ZenConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.opencode.ai/v1",
    val model: String = "opencode/big-pickle",
    val temperature: Double = 0.7
)
