package com.asc.gymgenie.ai

import com.asc.gymgenie.user.UserProfileResponse
import com.asc.gymgenie.user.UserProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Steps in the AI workout flow. Encoded as a sealed enum so the presenter
 * exposes a single source of truth for navigation between the 5 internal
 * pages and the UI layer just renders whichever one is current.
 */
enum class AiFlowStep(val index: Int) {
    CHOOSE(0),
    PROFILE(1),
    EXPERIENCE(2),
    HEALTH(3),
    CHAT(4),
}

/**
 * Profile information collected through screens 2-4.
 *
 * Numeric fields default to `0` so the slider UI can use a single
 * "any value > 0 is valid" rule. The textual answers default to empty
 * strings, which the picker UI treats as "not picked".
 */
data class AiProfileData(
    val age: Int = 0,
    val height: Int = 0,
    val weight: Int = 0,
    val experience: String = "",
    val frequency: String = "",
    val hasLimitations: String = "",
    val limitationsDesc: String = "",
) {
    val isProfileFilled: Boolean
        get() = age > 0 && height > 0 && weight > 0

    val isExperienceFilled: Boolean
        get() = experience.isNotBlank() && frequency.isNotBlank()

    val isHealthFilled: Boolean
        get() = when (hasLimitations) {
            HEALTH_NO -> true
            HEALTH_YES -> limitationsDesc.isNotBlank()
            else -> false
        }

    /**
     * Health context appended to every chat message. Empty when the user
     * has no limitations — the backend then receives `null` for that field.
     */
    fun healthContext(): String? = limitationsDesc.takeIf { it.isNotBlank() }

    companion object {
        const val HEALTH_YES = "Да"
        const val HEALTH_NO = "Нет"
        fun empty() = AiProfileData()
    }
}

/**
 * One bubble in the chat transcript. The `role` is intentionally a string
 * to keep the model trivially round-trippable to platform UI without
 * dragging the [AiResponseType] enum into both Compose and SwiftUI.
 */
data class AiChatMessage(
    val role: Role,
    val text: String,
) {
    enum class Role { USER, AI }
}

data class AiUiState(
    val step: AiFlowStep = AiFlowStep.CHOOSE,
    val profile: AiProfileData = AiProfileData(),
    val messages: List<AiChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val lastWorkout: AiWorkoutDto? = null,
    val savedPlanId: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Presenter for the AI workout flow.
 *
 * Owns:
 *  - step navigation between the 5 in-feature pages
 *  - the profile data captured before chat starts
 *  - the chat transcript, typing indicator, and last-generated workout
 *  - calls into [AiApi] for `chat`, `saveWorkout`, and `replaceWorkout`
 *
 * Pre-fills [AiProfileData] from [UserProfileStore] so a returning user does
 * not have to re-enter age / height / weight / experience / frequency / health
 * limitations on every visit. The user can still freely edit any field.
 *
 * The view layer is purely declarative: it observes [state] and dispatches
 * intents (`goTo`, `updateProfile`, `sendMessage`, `saveWorkout`, `reset`).
 *
 * Lifetime: callers must invoke [onCleared] when the surface is disposed
 * so in-flight coroutines are cancelled. Android does this by tying the
 * VM to a `remember` block; iOS does it from the wrapper's `deinit`.
 */
class AiViewModel(
    private val aiApi: AiApi,
    private val userProfileStore: UserProfileStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AiUiState())
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    init {
        val stored = userProfileStore.profile.value
        if (stored != null) {
            _state.update { it.copy(profile = stored.toAiProfileData()) }
        } else {
            scope.launch {
                val first = userProfileStore.profile.firstOrNull { it != null }
                if (first != null && _state.value.profile == AiProfileData()) {
                    _state.update { it.copy(profile = first.toAiProfileData()) }
                }
            }
        }
    }

    fun goTo(step: AiFlowStep) {
        _state.update { it.copy(step = step, errorMessage = null) }
    }

    fun goBack() {
        val current = _state.value.step
        val previous = AiFlowStep.entries.firstOrNull { it.index == current.index - 1 }
            ?: return
        goTo(previous)
    }

    fun updateProfile(transform: (AiProfileData) -> AiProfileData) {
        _state.update { it.copy(profile = transform(it.profile)) }
    }

    fun setAge(value: Int) = _state.update { it.copy(profile = it.profile.copy(age = value)) }
    fun setHeight(value: Int) = _state.update { it.copy(profile = it.profile.copy(height = value)) }
    fun setWeight(value: Int) = _state.update { it.copy(profile = it.profile.copy(weight = value)) }
    fun setExperience(value: String) = _state.update { it.copy(profile = it.profile.copy(experience = value)) }
    fun setFrequency(value: String) = _state.update { it.copy(profile = it.profile.copy(frequency = value)) }
    fun setHasLimitations(value: String) {
        _state.update {
            it.copy(
                profile = it.profile.copy(
                    hasLimitations = value,
                    limitationsDesc = if (value == AiProfileData.HEALTH_NO) "" else it.profile.limitationsDesc,
                ),
            )
        }
    }
    fun setLimitationsDesc(value: String) = _state.update { it.copy(profile = it.profile.copy(limitationsDesc = value)) }

    /**
     * Sends a user-typed message to the AI. Appends the user bubble first
     * (so the input feels responsive), turns on the typing indicator, then
     * mutates the state again with the assistant's reply.
     *
     * Profile fields are sent on every request: the backend keeps server-side
     * session state and ignores them after the first message, but resending
     * keeps the wire contract idempotent and lets the backend rebuild context
     * if its session expires.
     *
     * If the response is a [AiResponseType.WORKOUT] we cache it on state
     * so the UI can render the "save workout" CTA. Subsequent messages are
     * still allowed: the user may keep refining the plan, and only the
     * latest workout is offered for saving — but [AiUiState.savedPlanId] is
     * preserved so further saves go through `replaceWorkout` against the
     * same persisted plan instead of creating duplicates.
     */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isTyping) return

        val userBubble = AiChatMessage(role = AiChatMessage.Role.USER, text = trimmed)
        _state.update {
            it.copy(
                messages = it.messages + userBubble,
                isTyping = true,
                errorMessage = null,
                // A new message invalidates the previous "saved" badge so the
                // user can save (or update) a fresh plan if the AI returns a
                // different one. `savedPlanId` is intentionally preserved so
                // subsequent saves overwrite the existing plan.
                isSaved = false,
            )
        }

        val profile = _state.value.profile
        scope.launch {
            val result = aiApi.chat(
                AiChatRequest(
                    message = trimmed,
                    ageYears = profile.age.takeIf { it > 0 },
                    heightCm = profile.height.takeIf { it > 0 }?.toDouble(),
                    weightKg = profile.weight.takeIf { it > 0 }?.toDouble(),
                    experience = profile.experience.takeIf { it.isNotBlank() },
                    frequency = profile.frequency.takeIf { it.isNotBlank() },
                    healthIssues = profile.healthContext(),
                ),
            )
            result.fold(
                onSuccess = { response ->
                    val aiBubble = AiChatMessage(role = AiChatMessage.Role.AI, text = response.message)
                    _state.update {
                        it.copy(
                            messages = it.messages + aiBubble,
                            isTyping = false,
                            lastWorkout = response.workout ?: it.lastWorkout,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isTyping = false,
                            errorMessage = error.message ?: "Не удалось получить ответ",
                        )
                    }
                },
            )
        }
    }

    /**
     * Persists the last AI-generated workout. Idempotent at the UI level:
     * once `isSaved == true` the screen replaces the CTA with a success
     * banner and only un-sets the flag when a new AI message arrives.
     *
     * If the user has already saved this conversation's plan once
     * ([AiUiState.savedPlanId] is non-null), subsequent saves go through
     * [AiApi.replaceWorkout] and update the same persisted entry instead
     * of creating new copies.
     */
    fun saveWorkout() {
        val workout = _state.value.lastWorkout ?: return
        if (_state.value.isSaving || _state.value.isSaved) return

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        val request = SaveWorkoutRequest(
            exercises = workout.exercises,
            name = workout.name,
            description = workout.description,
        )

        val existingPlanId = _state.value.savedPlanId
        scope.launch {
            val result = if (existingPlanId == null) {
                aiApi.saveWorkout(request).map { it.workoutPlanId }
            } else {
                aiApi.replaceWorkout(existingPlanId, request).map { it.workoutPlanId }
            }
            result.fold(
                onSuccess = { planId ->
                    _state.update { it.copy(isSaving = false, isSaved = true, savedPlanId = planId) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Не удалось сохранить тренировку",
                        )
                    }
                },
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Resets the entire flow back to the starting screen. Also fires a
     * best-effort server-side session clear so the AI doesn't continue from
     * a stale conversation if the user starts over.
     */
    fun reset() {
        _state.value = AiUiState()
        scope.launch { aiApi.clearSession() }
    }

    fun onCleared() {
        scope.cancel()
    }
}

private fun UserProfileResponse.toAiProfileData() = AiProfileData(
    age = ageYears ?: 0,
    height = heightCm?.toInt() ?: 0,
    weight = weightKg?.toInt() ?: 0,
    experience = experience ?: "",
    frequency = frequency ?: "",
    // Empty string means "not yet answered"; only pre-select HEALTH_NO if the user
    // explicitly stored healthIssues = "" (empty, not null) — but since null and ""
    // are indistinguishable in the current schema, leave it unset so the user
    // must make a conscious choice on the Health screen.
    hasLimitations = if (!healthIssues.isNullOrBlank()) AiProfileData.HEALTH_YES else "",
    limitationsDesc = healthIssues ?: "",
)
