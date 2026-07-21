package com.forgeidea.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.domain.model.ChatRole
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.Message
import com.forgeidea.domain.usecase.SendMessageUseCase
import com.forgeidea.tools.ExecuteCommandTool
import com.forgeidea.tools.ToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val apiKeyStore: ApiKeyStore,
    private val executeCommandTool: ExecuteCommandTool
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedModelId = MutableStateFlow(apiKeyStore.getSelectedModelId())
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    private val _models = MutableStateFlow(apiKeyStore.getModels())
    val models: StateFlow<List<LlmModel>> = _models.asStateFlow()

    init {
        refreshModels()
    }

    fun refreshModels() {
        val currentModels = apiKeyStore.getModels()
        _models.value = currentModels
        val currentId = _selectedModelId.value
        if (currentModels.none { it.id == currentId }) {
            val fallback = currentModels.firstOrNull()?.id ?: ""
            _selectedModelId.value = fallback
            if (fallback.isNotBlank()) apiKeyStore.setSelectedModelId(fallback)
        }
    }

    fun selectModel(id: String) {
        _selectedModelId.value = id
        apiKeyStore.setSelectedModelId(id)
    }

    fun sendUserMessage(text: String) {
        if (_isStreaming.value) return

        val userMsg = Message(
            id = UUID.randomUUID().toString(),
            sessionId = "default",
            role = ChatRole.USER,
            content = text,
            timestamp = System.currentTimeMillis()
        )
        _messages.update { it + userMsg }

        if (text.startsWith("/exec ")) {
            val command = text.removePrefix("/exec ")
            handleCommandExecution(command)
            return
        }

        val assistantMsg = Message(
            id = UUID.randomUUID().toString(),
            sessionId = "default",
            role = ChatRole.ASSISTANT,
            content = "",
            reasoning = "",
            timestamp = System.currentTimeMillis()
        )
        _messages.update { it + assistantMsg }
        _isStreaming.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                sendMessageUseCase(
                    history = _messages.value.filter { it.id != assistantMsg.id && it.id != userMsg.id },
                    userInput = text,
                    model = _selectedModelId.value
                ).collect { chunk ->
                    _messages.update { msgs ->
                        msgs.map {
                            if (it.id == assistantMsg.id) {
                                it.copy(
                                    content = it.content + chunk.content,
                                    reasoning = it.reasoning + chunk.reasoning
                                )
                            } else it
                        }
                    }
                }
                val finalAssistant = _messages.value.find { it.id == assistantMsg.id }
                if (finalAssistant != null && finalAssistant.content.isBlank() && finalAssistant.reasoning.isBlank()) {
                    _messages.update { msgs ->
                        msgs.map { if (it.id == assistantMsg.id) it.copy(content = "❌ 没有收到回复内容") else it }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendUserMessage failed", e)
                _error.value = e.message ?: "未知错误"
                _messages.update { msgs ->
                    msgs.map { if (it.id == assistantMsg.id) it.copy(content = "❌ ${e.message ?: "请求失败"}") else it }
                }
            } finally {
                _isStreaming.value = false
            }
        }
    }

    private fun handleCommandExecution(command: String) {
        _isStreaming.value = true
        val assistantMsg = Message(
            id = UUID.randomUUID().toString(),
            sessionId = "default",
            role = ChatRole.ASSISTANT,
            content = "",
            reasoning = "",
            timestamp = System.currentTimeMillis()
        )
        _messages.update { it + assistantMsg }
        viewModelScope.launch {
            try {
                val args = buildJsonObject { put("command", JsonPrimitive(command)) }
                val result = executeCommandTool.execute(args)
                val content = when (result) {
                    is ToolResult.Success -> {
                        val stdout = result.data["stdout"]?.toString() ?: ""
                        val stderr = result.data["stderr"]?.toString() ?: ""
                        val exitCode = result.data["exitCode"]?.toString() ?: ""
                        buildString {
                            if (stdout.isNotBlank()) appendLine("stdout:\n$stdout")
                            if (stderr.isNotBlank()) appendLine("stderr:\n$stderr")
                            append("exitCode: $exitCode")
                        }
                    }
                    is ToolResult.Error -> "❌ ${result.message}"
                }
                _messages.update { msgs ->
                    msgs.map { if (it.id == assistantMsg.id) it.copy(content = content) else it }
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleCommandExecution failed", e)
                _messages.update { msgs ->
                    msgs.map { if (it.id == assistantMsg.id) it.copy(content = "❌ ${e.message ?: "执行失败"}") else it }
                }
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
