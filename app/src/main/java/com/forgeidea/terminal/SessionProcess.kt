package com.forgeidea.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionProcess(private val process: Process) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _stdout = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val stdout: SharedFlow<String> = _stdout.asSharedFlow()

    private val _stderr = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val stderr: SharedFlow<String> = _stderr.asSharedFlow()

    private val _exitCode = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val exitCode: SharedFlow<Int> = _exitCode.asSharedFlow()

    val isAlive: Boolean get() = process.isAlive

    init {
        scope.launch {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { _stdout.tryEmit(it) }
            }
        }
        scope.launch {
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { _stderr.tryEmit(it) }
            }
        }
        scope.launch {
            val code = process.waitFor()
            _exitCode.tryEmit(code)
            scope.cancel()
        }
    }

    fun destroy() {
        process.destroyForcibly()
        scope.cancel()
    }

    suspend fun waitFor(): Int = withContext(Dispatchers.IO) {
        process.waitFor()
    }
}
