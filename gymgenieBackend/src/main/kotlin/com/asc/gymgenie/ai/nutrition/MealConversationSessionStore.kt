package com.asc.gymgenie.ai.nutrition

import com.asc.gymgenie.ai.client.dto.GigaChatMessage
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-user GigaChat conversation memory for the meal-planning flow.
 *
 * Why a separate store (vs. namespacing keys inside [com.asc.gymgenie.ai.service.ConversationSessionStore]):
 *  - The meal flow has a different system prompt, different output contract
 *    (`meal_plan` vs `workout`), and different lifecycle from the workout
 *    flow. Sharing the same map by overloading keys with a string prefix
 *    couples two unrelated dialogs and risks accidental cross-bleed if
 *    callers mistype a prefix.
 *  - A dedicated component keeps the workout store's API and behaviour
 *    identical and lets each flow evolve independently (e.g. distinct
 *    `MAX_HISTORY` budgets, distinct trimming heuristics).
 *
 * The implementation mirrors the workout store: synchronized writes,
 * atomic init, system message at index 0 is preserved when trimming.
 */
@Component
class MealConversationSessionStore {

    private val sessions = ConcurrentHashMap<UUID, MutableList<GigaChatMessage>>()

    fun getHistory(userId: UUID): List<GigaChatMessage> =
        sessions[userId]?.toList() ?: emptyList()

    fun isEmpty(userId: UUID): Boolean =
        sessions[userId].isNullOrEmpty()

    /**
     * Atomically initializes the session with the given messages only if it is empty.
     * Returns true if initialized, false if a concurrent call beat us to it.
     */
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
            // Always preserve the system message at index 0
            val excess = history.size - MAX_HISTORY
            repeat(excess) { history.removeAt(1) }
        }
    }

    fun clearSession(userId: UUID) {
        sessions.remove(userId)
    }

    companion object {
        private const val MAX_HISTORY = 21 // system + 10 user/assistant pairs
    }
}
