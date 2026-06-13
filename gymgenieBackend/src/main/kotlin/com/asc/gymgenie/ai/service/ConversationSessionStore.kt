package com.asc.gymgenie.ai.service

import com.asc.gymgenie.ai.client.dto.GigaChatMessage
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class ConversationSessionStore {

    private val sessions = ConcurrentHashMap<UUID, MutableList<GigaChatMessage>>()

    fun getHistory(userId: UUID): List<GigaChatMessage> =
        sessions[userId]?.toList() ?: emptyList()

    fun isEmpty(userId: UUID): Boolean =
        sessions[userId].isNullOrEmpty()

    @Synchronized
    fun initializeIfEmpty(userId: UUID, vararg messages: GigaChatMessage): Boolean {
        if (!sessions[userId].isNullOrEmpty()) return false
        val history = sessions.getOrPut(userId) { mutableListOf() }
        history.addAll(messages)
        return true
    }

    @Synchronized
    fun addMessages(userId: UUID, vararg messages: GigaChatMessage) {
        val history = sessions.getOrPut(userId) { mutableListOf() }
        history.addAll(messages)
        if (history.size > MAX_HISTORY) {

            val excess = history.size - MAX_HISTORY
            repeat(excess) { history.removeAt(1) }
        }
    }

    fun clearSession(userId: UUID) {
        sessions.remove(userId)
    }

    companion object {
        private const val MAX_HISTORY = 21
    }
}
