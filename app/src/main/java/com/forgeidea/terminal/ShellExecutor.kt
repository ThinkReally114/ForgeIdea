package com.forgeidea.terminal

import com.forgeidea.domain.model.CommandRecord
import kotlinx.coroutines.flow.Flow

interface ShellExecutor {
    suspend fun execute(command: String, cwd: String? = null): Result<CommandRecord>
    fun executeStreaming(command: String, cwd: String? = null): Flow<ShellOutputLine>
    fun isEnvironmentReady(): Boolean
    suspend fun prepareEnvironment(): Flow<PrepareProgress>

    data class ShellOutputLine(
        val type: LineType,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        enum class LineType { STDOUT, STDERR, EXIT }
    }

    data class PrepareProgress(
        val message: String,
        val progress: Float,
        val isDone: Boolean = false,
        val error: String? = null
    )
}
