package com.forgeidea.domain.model

data class CommandRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = exitCode == 0
)

sealed class TerminalState {
    data object Unknown : TerminalState()
    data object Ready : TerminalState()
    data class Preparing(val message: String, val progress: Float) : TerminalState()
    data class Error(val message: String) : TerminalState()
}
