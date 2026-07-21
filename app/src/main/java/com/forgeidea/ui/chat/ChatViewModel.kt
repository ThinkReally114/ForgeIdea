package com.forgeidea.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.domain.model.ChatRole
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.Message
import com.forgeidea.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val apiKeyStore: ApiKeyStore
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
            } catch (e: Exception) {
                _error.value = e.message ?: "未知错误"
                _messages.update { msgs ->
                    msgs.map { if (it.id == assistantMsg.id) it.copy(content = "❌ ${e.message ?: "请求失败"}") else it }
                }
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
