package com.asc.gymgenie.nutrition

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
 * Steps in the AI meal-planning flow. Encoded as an enum with explicit
 * indices so the presenter exposes a single source of truth for navigation
 * between the 5 internal pages and the UI layer just renders whichever one is
 * current. Indices are also used by the iOS view layer to drive directional
 * step transitions.
 */
enum class AiMealFlowStep(val index: Int) {
    CHOOSE(0),
    PROFILE(1),
    GOAL(2),
    RESTRICTIONS(3),
    CHAT(4),
}

/**
 * Profile data captured through screens 2-4 of the AI meal flow.
 *
 * Numeric fields default to `0` so the slider UI can use a single
 * "any value > 0 is valid" rule. The textual answers default to empty
 * strings, which the picker UI treats as "not picked".
 */
data class AiMealProfileData(
    val age: Int = 0,
    val height: Int = 0,
    val weight: Int = 0,
) {
    val isProfileFilled: Boolean
        get() = age > 0 && height > 0 && weight > 0

    companion object {
        fun empty() = AiMealProfileData()
    }
}

/**
 * One bubble in the chat transcript. The role is intentionally an inner enum
 * so the model is trivially round-trippable to platform UI without dragging
 * the wire-level [AiMealResponseType] into Compose / SwiftUI.
 */
data class AiMealChatMessage(
    val role: Role,
    val text: String,
) {
    enum class Role { USER, AI }
}

data class AiMealUiState(
    val step: AiMealFlowStep = AiMealFlowStep.PROFILE,
    val profile: AiMealProfileData = AiMealProfileData(),
    /** Wire string of the picked [MealGoal], empty until the user picks one. */
    val goal: String = "",
    val dietaryRestrictions: String = "",
    val allergies: String = "",
    val messages: List<AiMealChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val lastMealPlan: AiMealPlanData? = null,
    val savedPlanId: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Presenter for the AI meal-planning flow.
 *
 * Owns:
 *  - step navigation between the 5 in-feature pages
 *  - the profile + goal + dietary data captured before chat starts
 *  - the chat transcript, typing indicator, and last-generated meal plan
 *  - calls into [AiMealApi] for `chat`, `saveMealPlan`, and `replaceMealPlan`
 *
 * Pre-fills [AiMealProfileData] from [UserProfileStore] so a returning user
 * does not have to re-enter age / height / weight on every visit. The user
 * can still freely edit any field. Goal / restrictions / allergies are not
 * pre-filled because the user profile does not currently store them.
 *
 * The view layer is purely declarative: it observes [state] and dispatches
 * intents (`goTo`, `setGoal`, `sendMessage`, `saveMealPlan`, `reset`).
 *
 * Lifetime: callers must invoke [onCleared] when the surface is disposed so
 * in-flight coroutines are cancelled. Android does this by tying the VM to
 * a `remember` block; iOS does it from the wrapper's `deinit`.
 */
class AiMealViewModel(
    private val aiMealApi: AiMealApi,
    private val userProfileStore: UserProfileStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AiMealUiState())
    val state: StateFlow<AiMealUiState> = _state.asStateFlow()

    init {
        val stored = userProfileStore.profile.value
        if (stored != null) {
            _state.update { it.copy(profile = stored.toAiMealProfileData()) }
        } else {
            scope.launch {
                val first = userProfileStore.profile.firstOrNull { it != null }
                if (first != null && _state.value.profile == AiMealProfileData()) {
                    _state.update { it.copy(profile = first.toAiMealProfileData()) }
                }
            }
        }
    }

    fun goTo(step: AiMealFlowStep) {
        _state.update { it.copy(step = step, errorMessage = null) }
    }

    fun goBack() {
        val current = _state.value.step
        val previous = AiMealFlowStep.entries.firstOrNull { it.index == current.index - 1 }
            ?: return
        goTo(previous)
    }

    /** Stores the picked goal as a wire value (e.g. `"LOSE_WEIGHT"`). */
    fun setGoal(value: String) = _state.update { it.copy(goal = value) }

    fun setDietaryRestrictions(value: String) =
        _state.update { it.copy(dietaryRestrictions = value) }

    fun setAllergies(value: String) =
        _state.update { it.copy(allergies = value) }

    fun setAge(value: Int) =
        _state.update { it.copy(profile = it.profile.copy(age = value)) }

    fun setHeight(value: Int) =
        _state.update { it.copy(profile = it.profile.copy(height = value)) }

    fun setWeight(value: Int) =
        _state.update { it.copy(profile = it.profile.copy(weight = value)) }

    /**
     * Idempotent profile pre-fill from the cached [UserProfileResponse].
     *
     * Safe to call repeatedly: only fills numeric slots that are still at
     * their default `0` value, so it cannot overwrite explicit edits the
     * user has already made on the profile screen.
     */
    fun prefillProfile() {
        val cached = userProfileStore.profile.value ?: return
        _state.update { current ->
            val next = current.profile.copy(
                age = current.profile.age.takeIf { it > 0 } ?: (cached.ageYears ?: 0),
                height = current.profile.height.takeIf { it > 0 }
                    ?: (cached.heightCm?.toInt() ?: 0),
                weight = current.profile.weight.takeIf { it > 0 }
                    ?: (cached.weightKg?.toInt() ?: 0),
            )
            current.copy(profile = next)
        }
    }

    /**
     * Sends a user-typed message to the AI. Appends the user bubble first
     * (so the input feels responsive), turns on the typing indicator, then
     * mutates the state again with the assistant's reply.
     *
     * Profile / goal / dietary fields are sent on every request: the backend
     * keeps server-side session state and may ignore them after the first
     * message, but resending keeps the wire contract idempotent and lets the
     * backend rebuild context if its session expires.
     *
     * If the response is a [AiMealResponseType.MEAL_PLAN] we cache it on
     * state so the UI can render the "save plan" CTA. Subsequent messages are
     * still allowed: the user may keep refining the plan, and only the
     * latest plan is offered for saving — but [AiMealUiState.savedPlanId] is
     * preserved so further saves go through `replaceMealPlan` against the
     * same persisted plan instead of creating duplicates.
     */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isTyping) return

        val userBubble = AiMealChatMessage(role = AiMealChatMessage.Role.USER, text = trimmed)
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

        val state = _state.value
        scope.launch {
            val result = aiMealApi.chat(
                AiMealChatRequest(
                    message = trimmed,
                    ageYears = state.profile.age.takeIf { it > 0 },
                    // The picker UI captures whole-number cm / kg, but the
                    // wire contract is Double to match the backend column
                    // type. Convert on send rather than storing as Double on
                    // [AiMealProfileData] to keep the slider state simple.
                    heightCm = state.profile.height.takeIf { it > 0 }?.toDouble(),
                    weightKg = state.profile.weight.takeIf { it > 0 }?.toDouble(),
                    goal = state.goal.takeIf { it.isNotBlank() },
                    dietaryRestrictions = state.dietaryRestrictions.takeIf { it.isNotBlank() },
                    allergies = state.allergies.takeIf { it.isNotBlank() },
                ),
            )
            result.fold(
                onSuccess = { response ->
                    val aiBubble = AiMealChatMessage(
                        role = AiMealChatMessage.Role.AI,
                        text = response.message,
                    )
                    _state.update {
                        it.copy(
                            messages = it.messages + aiBubble,
                            isTyping = false,
                            lastMealPlan = response.mealPlan ?: it.lastMealPlan,
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
     * Persists the last AI-generated meal plan. Idempotent at the UI level:
     * once `isSaved == true` the screen replaces the CTA with a success
     * banner and only un-sets the flag when a new AI message arrives.
     *
     * If the user has already saved this conversation's plan once
     * ([AiMealUiState.savedPlanId] is non-null), subsequent saves go through
     * [AiMealApi.replaceMealPlan] and update the same persisted entry instead
     * of creating new copies.
     */
    fun saveMealPlan() {
        val plan = _state.value.lastMealPlan ?: return
        if (_state.value.isSaving || _state.value.isSaved) return

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        // The user's explicit goal selection takes precedence over whatever
        // the AI may (or may not) have echoed back inside the plan payload.
        val goal = _state.value.goal.takeIf { it.isNotBlank() } ?: plan.goal
        val request = SaveMealPlanRequest(
            name = plan.name,
            description = plan.description,
            goal = goal,
            totalCalories = plan.totalCalories,
            meals = plan.meals,
        )

        val existingPlanId = _state.value.savedPlanId
        scope.launch {
            val result = if (existingPlanId == null) {
                aiMealApi.saveMealPlan(request).map { it.mealPlanId }
            } else {
                aiMealApi.replaceMealPlan(existingPlanId, request).map { it.mealPlanId }
            }
            result.fold(
                onSuccess = { planId ->
                    _state.update {
                        it.copy(isSaving = false, isSaved = true, savedPlanId = planId)
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Не удалось сохранить рацион",
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
        _state.value = AiMealUiState()
        scope.launch { aiMealApi.clearSession() }
    }

    fun onCleared() {
        scope.cancel()
    }
}

private fun UserProfileResponse.toAiMealProfileData() = AiMealProfileData(
    age = ageYears ?: 0,
    height = heightCm?.toInt() ?: 0,
    weight = weightKg?.toInt() ?: 0,
)
