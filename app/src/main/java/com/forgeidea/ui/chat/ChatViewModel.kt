package com.forgeidea.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.data.local.entity.SessionEntity
import com.forgeidea.data.repository.ChatRepository
import com.forgeidea.domain.model.ChatRole
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.Message
import com.forgeidea.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val apiKeyStore: ApiKeyStore,
    private val chatRepository: ChatRepository
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

    private val _currentSessionId = MutableStateFlow<String?>(apiKeyStore.getCurrentSessionId())
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions.asStateFlow()

    private var messageObserverJob: Job? = null

    init {
        refreshModels()
        observeSessions()
        loadCurrentSession()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            chatRepository.observeSessions().collect { sessions ->
                _sessions.value = sessions
            }
        }
    }

    private fun loadCurrentSession() {
        messageObserverJob?.cancel()
        messageObserverJob = viewModelScope.launch {
            val sessionId = _currentSessionId.value
                ?: chatRepository.getCurrentOrCreateSession(_selectedModelId.value).also {
                    _currentSessionId.value = it.id
                }.id
            chatRepository.observeMessages(sessionId).collect { msgs ->
                _messages.value = msgs
            }
        }
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

    fun createNewSession() {
        viewModelScope.launch {
            val session = chatRepository.createNewSession(_selectedModelId.value)
            _currentSessionId.value = session.id
            _messages.value = emptyList()
            messageObserverJob?.cancel()
            messageObserverJob = viewModelScope.launch {
                chatRepository.observeMessages(session.id).collect { msgs ->
                    _messages.value = msgs
                }
            }
        }
    }

    fun switchToSession(id: String) {
        viewModelScope.launch {
            val session = chatRepository.switchToSession(id) ?: return@launch
            _currentSessionId.value = session.id
            _selectedModelId.value = session.modelId
            messageObserverJob?.cancel()
            messageObserverJob = viewModelScope.launch {
                chatRepository.observeMessages(session.id).collect { msgs ->
                    _messages.value = msgs
                }
            }
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(id)
            if (_currentSessionId.value == id) {
                _currentSessionId.value = null
                loadCurrentSession()
            }
        }
    }

    fun sendUserMessage(text: String) {
        if (_isStreaming.value) return
        val sessionId = _currentSessionId.value ?: return

        val userMsg = Message(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = ChatRole.USER,
            content = text,
            timestamp = System.currentTimeMillis()
        )

        val assistantId = UUID.randomUUID().toString()
        val assistantMsg = Message(
            id = assistantId,
            sessionId = sessionId,
            role = ChatRole.ASSISTANT,
            content = "",
            reasoning = "",
            timestamp = System.currentTimeMillis() + 1
        )

        viewModelScope.launch {
            try {
                chatRepository.addMessage(sessionId, userMsg)
                chatRepository.addMessage(sessionId, assistantMsg)
                _isStreaming.value = true
                _error.value = null

                val history = _messages.value.filter { it.id != assistantId }
                var currentContent = ""
                var currentReasoning = ""
                sendMessageUseCase(
                    history = history,
                    userInput = text,
                    model = _selectedModelId.value
                ).collect { chunk ->
                    currentContent += chunk.content
                    currentReasoning += chunk.reasoning
                    val updated = assistantMsg.copy(
                        content = currentContent,
                        reasoning = currentReasoning
                    )
                    chatRepository.updateMessage(sessionId, updated)
                }

                if (currentContent.isBlank() && currentReasoning.isBlank()) {
                    val errMsg = assistantMsg.copy(content = "❌ 没有收到回复内容")
                    chatRepository.updateMessage(sessionId, errMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendUserMessage failed", e)
                _error.value = e.message ?: "未知错误"
                val errMsg = assistantMsg.copy(content = "❌ ${e.message ?: "请求失败"}")
                chatRepository.updateMessage(sessionId, errMsg)
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
