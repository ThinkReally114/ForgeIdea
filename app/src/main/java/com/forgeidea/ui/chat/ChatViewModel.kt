package com.forgeidea.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.data.repository.ChatRepository
import com.forgeidea.domain.model.ChatRole
import com.forgeidea.domain.model.LlmModel
import com.forgeidea.domain.model.Message
import com.forgeidea.domain.model.Provider
import com.forgeidea.domain.usecase.SendMessageUseCase
import com.forgeidea.tools.ExecuteCommandTool
import com.forgeidea.tools.WorkspaceFileTools
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val apiKeyStore: ApiKeyStore,
    private val chatRepository: ChatRepository,
    private val executeCommandTool: ExecuteCommandTool,
    private val workspaceFileTools: WorkspaceFileTools
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

    private val _providers = MutableStateFlow(apiKeyStore.getProviders())
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(apiKeyStore.getCurrentSessionId())
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _sessions = MutableStateFlow<List<com.forgeidea.data.local.entity.SessionEntity>>(emptyList())
    val sessions: StateFlow<List<com.forgeidea.data.local.entity.SessionEntity>> = _sessions.asStateFlow()

    private val _workspaceFiles = MutableStateFlow<List<String>>(emptyList())
    val workspaceFiles: StateFlow<List<String>> = _workspaceFiles.asStateFlow()

    init {
        viewModelScope.launch {
            _currentSessionId.value?.let { loadSession(it) }
            chatRepository.observeSessions().collect { list ->
                _sessions.value = list.sortedByDescending { it.updatedAt }
            }
        }
    }

    fun refreshModels() {
        val currentModels = apiKeyStore.getModels()
        _models.value = currentModels
        _providers.value = apiKeyStore.getProviders()
        val currentId = _selectedModelId.value
        if (currentModels.none { it.id == currentId }) {
            val fallback = currentModels.firstOrNull()?.id ?: ""
            _selectedModelId.value = fallback
            if (fallback.isNotBlank()) apiKeyStore.setSelectedModelId(fallback)
        }
    }

    fun loadSession(id: String) {
        viewModelScope.launch {
            val session = chatRepository.switchToSession(id) ?: return@launch
            _currentSessionId.value = session.id
            _selectedModelId.value = session.modelId.ifBlank { apiKeyStore.getSelectedModelId() }
            chatRepository.observeMessages(session.id).collect { list ->
                _messages.value = list
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val session = chatRepository.createNewSession(_selectedModelId.value)
            _currentSessionId.value = session.id
        }
    }

    fun selectModel(id: String) {
        _selectedModelId.value = id
        apiKeyStore.setSelectedModelId(id)
    }

    fun renameSession(id: String, title: String) {
        viewModelScope.launch {
            chatRepository.renameSession(id, title)
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(id)
            if (_currentSessionId.value == id) {
                _currentSessionId.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun sendUserMessage(text: String) {
        if (_isStreaming.value) return
        val sessionId = _currentSessionId.value ?: return
        val modelId = _selectedModelId.value
        val provider = apiKeyStore.getProviderForModel(modelId)
        if (provider == null) {
            _error.value = "未找到模型对应的服务商，请先配置"
            return
        }
        val modelName = _models.value.find { it.id == modelId }?.name ?: modelId

        val userMsg = Message(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = ChatRole.USER,
            content = text,
            timestamp = System.currentTimeMillis()
        )

        if (text.startsWith("/exec ")) {
            viewModelScope.launch {
                chatRepository.addMessage(sessionId, userMsg)
            }
            handleCommandExecution(sessionId, text.removePrefix("/exec "))
            return
        }

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
                val result = sendMessageUseCase.send(
                    sessionId = sessionId,
                    history = history,
                    userInput = text,
                    model = modelId,
                    provider = provider
                )

                val fullContent = result.content
                val fullReasoning = result.reasoning
                if (fullContent.isBlank() && fullReasoning.isBlank()) {
                    val errMsg = assistantMsg.copy(
                        content = "❌ 没有收到回复内容",
                        modelName = modelName,
                        providerName = provider.name,
                        durationMs = result.durationMs
                    )
                    chatRepository.updateMessage(sessionId, errMsg)
                } else if (fullContent.length <= 2) {
                    chatRepository.updateMessage(
                        sessionId,
                        assistantMsg.copy(
                            content = fullContent,
                            reasoning = fullReasoning,
                            modelName = modelName,
                            providerName = provider.name,
                            durationMs = result.durationMs
                        )
                    )
                } else {
                    val chunkSize = (fullContent.length / 40).coerceAtLeast(1)
                    var currentLength = 0
                    while (currentLength < fullContent.length) {
                        currentLength = (currentLength + chunkSize).coerceAtMost(fullContent.length)
                        chatRepository.updateMessage(
                            sessionId,
                            assistantMsg.copy(
                                content = fullContent.take(currentLength),
                                reasoning = if (currentLength >= fullContent.length) fullReasoning else "",
                                modelName = modelName,
                                providerName = provider.name,
                                durationMs = result.durationMs
                            )
                        )
                        delay(16)
                    }
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

    private fun handleCommandExecution(sessionId: String, command: String) {
        val assistantId = UUID.randomUUID().toString()
        val assistantMsg = Message(
            id = assistantId,
            sessionId = sessionId,
            role = ChatRole.ASSISTANT,
            content = "",
            timestamp = System.currentTimeMillis() + 1
        )

        viewModelScope.launch {
            try {
                chatRepository.addMessage(sessionId, assistantMsg)
                _isStreaming.value = true
                _error.value = null
                val result = executeCommandTool.execute(command)
                val output = result.getOrNull() ?: "❌ 命令执行失败"
                chatRepository.updateMessage(sessionId, assistantMsg.copy(content = output))
            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed", e)
                _error.value = e.message ?: "命令执行失败"
                chatRepository.updateMessage(sessionId, assistantMsg.copy(content = "❌ ${e.message}"))
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun refreshWorkspaceFiles() {
        val sessionId = _currentSessionId.value ?: return
        _workspaceFiles.value = workspaceFileTools.listFiles(sessionId)
    }

    fun createWorkspaceFile(path: String, content: String = "") {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            workspaceFileTools.createFile(sessionId, path, content)
            refreshWorkspaceFiles()
        }
    }

    fun deleteWorkspaceFile(path: String) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            workspaceFileTools.resolveWorkspaceFile(sessionId, path).deleteRecursively()
            refreshWorkspaceFiles()
        }
    }

    fun markMessageAnimated(messageId: String) {
        viewModelScope.launch {
            chatRepository.markMessageAnimated(messageId)
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
