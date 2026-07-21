package com.forgeidea.data.repository

import com.forgeidea.data.datastore.ApiKeyStore
import com.forgeidea.data.local.dao.MessageDao
import com.forgeidea.data.local.dao.SessionDao
import com.forgeidea.data.local.entity.MessageEntity
import com.forgeidea.data.local.entity.SessionEntity
import com.forgeidea.domain.model.ChatRole
import com.forgeidea.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
    private val apiKeyStore: ApiKeyStore
) {
    fun observeSessions(): Flow<List<SessionEntity>> = sessionDao.observeAll()

    fun observeMessages(sessionId: String): Flow<List<Message>> =
        messageDao.observeBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getCurrentOrCreateSession(modelId: String): SessionEntity {
        val currentId = apiKeyStore.getCurrentSessionId()
        if (currentId != null) {
            sessionDao.getById(currentId)?.let { return it }
        }
        val newSession = SessionEntity(
            id = UUID.randomUUID().toString(),
            title = "新对话",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            modelId = modelId,
            messageCount = 0
        )
        sessionDao.insert(newSession)
        apiKeyStore.setCurrentSessionId(newSession.id)
        return newSession
    }

    suspend fun createNewSession(modelId: String): SessionEntity {
        val session = SessionEntity(
            id = UUID.randomUUID().toString(),
            title = "新对话",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            modelId = modelId,
            messageCount = 0
        )
        sessionDao.insert(session)
        apiKeyStore.setCurrentSessionId(session.id)
        return session
    }

    suspend fun switchToSession(id: String): SessionEntity? {
        val session = sessionDao.getById(id) ?: return null
        apiKeyStore.setCurrentSessionId(id)
        return session
    }

    suspend fun deleteSession(id: String) {
        sessionDao.delete(id)
        if (apiKeyStore.getCurrentSessionId() == id) {
            apiKeyStore.setCurrentSessionId(null)
        }
    }

    suspend fun renameSession(id: String, title: String) {
        val session = sessionDao.getById(id) ?: return
        sessionDao.update(session.copy(title = title))
    }

    suspend fun addMessage(sessionId: String, message: Message) {
        messageDao.insert(message.toEntity(sessionId))
        val session = sessionDao.getById(sessionId) ?: return
        sessionDao.update(
            session.copy(
                updatedAt = System.currentTimeMillis(),
                messageCount = session.messageCount + 1,
                title = if (session.messageCount == 0 && message.role == ChatRole.USER) {
                    message.content.take(30)
                } else session.title
            )
        )
    }

    suspend fun updateMessage(sessionId: String, message: Message) {
        messageDao.insert(message.toEntity(sessionId))
    }

    suspend fun markMessageAnimated(messageId: String) {
        messageDao.updateAnimated(messageId, animated = true)
    }

    private fun MessageEntity.toDomain() = Message(
        id = id,
        sessionId = sessionId,
        role = runCatching { ChatRole.valueOf(role.uppercase()) }.getOrDefault(ChatRole.ASSISTANT),
        content = content,
        timestamp = timestamp,
        reasoning = reasoning,
        toolCallId = toolCallId,
        animated = animated,
        modelName = modelName,
        providerName = providerName,
        durationMs = durationMs
    )

    private fun Message.toEntity(sid: String) = MessageEntity(
        id = id,
        sessionId = sid,
        role = role.name.lowercase(),
        content = content,
        timestamp = timestamp,
        reasoning = reasoning,
        toolCallId = toolCallId,
        animated = animated,
        modelName = modelName,
        providerName = providerName,
        durationMs = durationMs
    )
}
