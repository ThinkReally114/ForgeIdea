package com.forgeidea.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forgeidea.domain.model.CommandRecord
import com.forgeidea.domain.model.TerminalState
import com.forgeidea.terminal.ShellExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalViewModel(private val shellExecutor: ShellExecutor) : ViewModel() {

    private val _state = MutableStateFlow<TerminalState>(TerminalState.Unknown)
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    private val _records = MutableStateFlow<List<CommandRecord>>(emptyList())
    val records: StateFlow<List<CommandRecord>> = _records.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    fun onInputChange(value: String) {
        _input.value = value
    }

    fun prepare() {
        viewModelScope.launch {
            _state.value = TerminalState.Preparing("准备环境中", 0f)
            shellExecutor.prepareEnvironment().collect { progress ->
                _state.value = if (progress.isDone) {
                    if (progress.error != null) TerminalState.Error(progress.error)
                    else TerminalState.Ready
                } else {
                    TerminalState.Preparing(progress.message, progress.progress)
                }
            }
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        _input.value = ""
        _isExecuting.value = true
        viewModelScope.launch {
            val result = shellExecutor.execute(command)
            result.onSuccess { record ->
                _records.value = _records.value + record
            }.onFailure { error ->
                _records.value = _records.value + CommandRecord(
                    command = command,
                    stdout = "",
                    stderr = error.message ?: "执行失败",
                    exitCode = -1,
                    isSuccess = false
                )
            }
            _isExecuting.value = false
        }
    }

    fun clearHistory() {
        _records.value = emptyList()
    }
}
