package com.qz.agent.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qz.agent.domain.model.ChatRole
import com.qz.agent.domain.model.Message
import com.qz.agent.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    fun sendUserMessage(text: String) {
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
            timestamp = System.currentTimeMillis()
        )
        _messages.update { it + assistantMsg }
        _isStreaming.value = true

        viewModelScope.launch {
            try {
                sendMessageUseCase(_messages.value.filter { it.id != assistantMsg.id }, text)
                    .collect { chunk ->
                        _messages.update { msgs ->
                            msgs.map { if (it.id == assistantMsg.id) it.copy(content = it.content + chunk.content) else it }
                        }
                    }
            } finally {
                _isStreaming.value = false
            }
        }
    }
}
