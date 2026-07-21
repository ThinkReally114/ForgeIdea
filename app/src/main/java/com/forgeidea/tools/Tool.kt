package com.forgeidea.tools

import kotlinx.serialization.json.JsonObject

interface Tool {
    val name: String
    val description: String
    val schema: JsonObject
    suspend fun execute(args: JsonObject): ToolResult
}

sealed class ToolResult {
    data class Success(val data: JsonObject) : ToolResult()
    data class Error(val message: String) : ToolResult()

    companion object {
        fun success(data: JsonObject): ToolResult = Success(data)
        fun error(message: String): ToolResult = Error(message)
    }
}
