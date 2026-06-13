package com.asc.gymgenie.nutrition

import com.asc.gymgenie.user.UpdateUserProfileRequest
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

enum class AiMealFlowStep(val index: Int) {
    CHOOSE(0),
    PROFILE(1),
    GOAL(2),
    RESTRICTIONS(3),
    CHAT(4),
}

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

data class AiMealChatMessage(
    val role: Role,
    val text: String,
) {
    enum class Role { USER, AI }
}

data class AiMealUiState(
    val step: AiMealFlowStep = AiMealFlowStep.CHOOSE,
    val profile: AiMealProfileData = AiMealProfileData(),

    val goal: String = "",
    val dietaryRestrictions: String = "",
    val allergies: String = "",

    val selectedMealType: AiMealType? = null,
    val messages: List<AiMealChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val lastMealPlan: AiMealPlanData? = null,
    val savedPlanId: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,

    val showSchedulePicker: Boolean = false,

    val scheduleMode: String = "ONE_OFF",

    val selectedDate: String? = null,

    val selectedWeekdays: List<String> = emptyList(),

    val bookedOneOffDates: List<String> = emptyList(),
    val bookedRecurringDays: List<String> = emptyList(),

    val conflicts: List<AiMealConflictPlan> = emptyList(),
    val showConflictDialog: Boolean = false,
)

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
        val current = _state.value.step
        if (step.index > current.index && current == AiMealFlowStep.PROFILE) {
            syncProfileIfChanged()
        }
        _state.update { it.copy(step = step, errorMessage = null) }
    }

    fun goBack() {
        val current = _state.value.step
        val previous = AiMealFlowStep.entries.firstOrNull { it.index == current.index - 1 }
            ?: return
        goTo(previous)
    }

    fun setGoal(value: String) = _state.update { it.copy(goal = value) }

    fun setMealType(type: AiMealType) {
        _state.update { it.copy(selectedMealType = type) }
    }

    fun selectMealType(type: AiMealType) {
        _state.update { it.copy(selectedMealType = type) }
        goTo(AiMealFlowStep.PROFILE)
    }

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

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isTyping) return

        val userBubble = AiMealChatMessage(role = AiMealChatMessage.Role.USER, text = trimmed)
        _state.update {
            it.copy(
                messages = it.messages + userBubble,
                isTyping = true,
                errorMessage = null,

                isSaved = false,
            )
        }

        val state = _state.value
        scope.launch {
            val result = aiMealApi.chat(
                AiMealChatRequest(
                    message = trimmed,
                    ageYears = state.profile.age.takeIf { it > 0 },

                    heightCm = state.profile.height.takeIf { it > 0 }?.toDouble(),
                    weightKg = state.profile.weight.takeIf { it > 0 }?.toDouble(),
                    goal = state.goal.takeIf { it.isNotBlank() },
                    dietaryRestrictions = state.dietaryRestrictions.takeIf { it.isNotBlank() },
                    allergies = state.allergies.takeIf { it.isNotBlank() },
                    mealType = state.selectedMealType?.wireValue,
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

    fun onAddPlanTapped() {
        if (_state.value.lastMealPlan == null) return
        _state.update { it.copy(showSchedulePicker = true, isSaved = false, errorMessage = null) }
        scope.launch {
            aiMealApi.getBookedDays(mealType = _state.value.selectedMealType?.wireValue).fold(
                onSuccess = { resp ->
                    _state.update {
                        it.copy(
                            bookedOneOffDates = resp.oneOffDates,
                            bookedRecurringDays = resp.recurringDays,
                        )
                    }
                },
                onFailure = {  },
            )
        }
    }

    fun setScheduleMode(mode: String) {
        _state.update { it.copy(scheduleMode = mode) }
    }

    fun setSelectedDate(date: String?) {
        _state.update { it.copy(selectedDate = date) }
    }

    fun toggleWeekday(day: String) {
        _state.update {
            val current = it.selectedWeekdays.toMutableList()
            if (day in current) current.remove(day) else current.add(day)
            it.copy(selectedWeekdays = current)
        }
    }

    fun dismissSchedulePicker() {
        _state.update {
            it.copy(
                showSchedulePicker = false,
                selectedDate = null,
                selectedWeekdays = emptyList(),
                conflicts = emptyList(),
                showConflictDialog = false,
            )
        }
    }

    fun saveWithSchedule() {
        val s = _state.value
        val plan = s.lastMealPlan ?: return

        val scheduleType: String
        val oneOffDate: String?
        val scheduleDays: List<String>

        if (s.scheduleMode == "ONE_OFF") {
            if (s.selectedDate == null) return
            scheduleType = "ONE_TIME"
            oneOffDate = s.selectedDate
            scheduleDays = emptyList()
        } else {
            if (s.selectedWeekdays.isEmpty()) return
            scheduleType = "RECURRING"
            oneOffDate = null
            scheduleDays = s.selectedWeekdays
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        scope.launch {

            val conflictResult = aiMealApi.checkConflicts(
                scheduleType = scheduleType,
                oneOffDate = oneOffDate,
                scheduleDays = scheduleDays,
                mealType = _state.value.selectedMealType?.wireValue,
            )
            conflictResult.fold(
                onSuccess = { resp ->
                    if (resp.hasConflicts) {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                conflicts = resp.conflicts,
                                showConflictDialog = true,
                            )
                        }
                    } else {
                        doSave(plan, scheduleType, oneOffDate, scheduleDays, emptyList())
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Не удалось проверить конфликты",
                        )
                    }
                },
            )
        }
    }

    fun confirmReplace() {
        val s = _state.value
        val plan = s.lastMealPlan ?: return
        val conflictIds = s.conflicts.map { it.planId }

        val scheduleType: String
        val oneOffDate: String?
        val scheduleDays: List<String>

        if (s.scheduleMode == "ONE_OFF") {
            if (s.selectedDate == null) return
            scheduleType = "ONE_TIME"
            oneOffDate = s.selectedDate
            scheduleDays = emptyList()
        } else {
            if (s.selectedWeekdays.isEmpty()) return
            scheduleType = "RECURRING"
            oneOffDate = null
            scheduleDays = s.selectedWeekdays
        }

        _state.update { it.copy(showConflictDialog = false, isSaving = true) }
        scope.launch {
            doSave(plan, scheduleType, oneOffDate, scheduleDays, conflictIds)
        }
    }

    fun dismissConflictDialog() {
        _state.update { it.copy(showConflictDialog = false, conflicts = emptyList()) }
    }

    private suspend fun doSave(
        plan: AiMealPlanData,
        scheduleType: String,
        oneOffDate: String?,
        scheduleDays: List<String>,
        replaceConflictPlanIds: List<String>,
    ) {
        val goal = _state.value.goal.takeIf { it.isNotBlank() } ?: plan.goal
        val request = SaveMealPlanRequest(
            name = plan.name,
            description = plan.description,
            goal = goal,
            totalCalories = plan.totalCalories,
            mealType = _state.value.selectedMealType?.wireValue,
            scheduleType = scheduleType,
            oneOffDate = oneOffDate,
            scheduleDays = scheduleDays,
            replaceConflictPlanIds = replaceConflictPlanIds,
            meals = plan.meals,
        )

        val existingPlanId = _state.value.savedPlanId
        val result = if (existingPlanId == null) {
            aiMealApi.saveMealPlan(request).map { it.mealPlanId }
        } else {
            aiMealApi.replaceMealPlan(existingPlanId, request).map { it.mealPlanId }
        }
        result.fold(
            onSuccess = { planId ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        isSaved = true,
                        savedPlanId = planId,
                        showSchedulePicker = false,
                        selectedDate = null,
                        selectedWeekdays = emptyList(),
                        conflicts = emptyList(),
                    )
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

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun syncProfileIfChanged() {
        val current = _state.value.profile
        val stored = userProfileStore.profile.value ?: return

        val ageChanged = current.age > 0 && current.age != (stored.ageYears ?: 0)
        val heightChanged = current.height > 0 && current.height != (stored.heightCm?.toInt() ?: 0)
        val weightChanged = current.weight > 0 && current.weight != (stored.weightKg?.toInt() ?: 0)

        if (!ageChanged && !heightChanged && !weightChanged) return

        val request = UpdateUserProfileRequest(
            ageYears = if (ageChanged) current.age else null,
            heightCm = if (heightChanged) current.height.toDouble() else null,
            weightKg = if (weightChanged) current.weight.toDouble() else null,
        )
        scope.launch { userProfileStore.saveProfile(request) }
    }

    fun refreshProfileFromStore() {
        if (_state.value.step != AiMealFlowStep.CHOOSE) return
        val stored = userProfileStore.profile.value ?: return
        _state.update { it.copy(profile = stored.toAiMealProfileData()) }
    }

    fun reset() {
        val stored = userProfileStore.profile.value
        _state.value = if (stored != null) {
            AiMealUiState(profile = stored.toAiMealProfileData())
        } else {
            AiMealUiState()
        }
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
