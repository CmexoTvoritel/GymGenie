package com.asc.gymgenie.ai

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

enum class AiFlowStep(val index: Int) {
    CHOOSE(0),
    PROFILE(1),
    EXPERIENCE(2),
    HEALTH(3),
    CHAT(4),
}

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

    fun healthContext(): String? = limitationsDesc.takeIf { it.isNotBlank() }

    companion object {
        const val HEALTH_YES = "Да"
        const val HEALTH_NO = "Нет"
        fun empty() = AiProfileData()
    }
}

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
        val current = _state.value.step
        if (step.index > current.index && current in PROFILE_STEPS) {
            syncProfileIfChanged()
        }
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

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isTyping) return

        val userBubble = AiChatMessage(role = AiChatMessage.Role.USER, text = trimmed)
        _state.update {
            it.copy(
                messages = it.messages + userBubble,
                isTyping = true,
                errorMessage = null,

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

    fun saveWorkout() {
        val workout = _state.value.lastWorkout ?: return
        if (_state.value.isSaving || _state.value.isSaved) return

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        val request = SaveWorkoutRequest(
            exercises = workout.exercises,
            name = workout.name,
            description = workout.description,
            restSeconds = workout.restSeconds,
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

    private fun syncProfileIfChanged() {
        val current = _state.value.profile
        val stored = userProfileStore.profile.value ?: return
        val request = buildProfileUpdateRequest(current, stored) ?: return
        scope.launch { userProfileStore.saveProfile(request) }
    }

    private fun buildProfileUpdateRequest(
        current: AiProfileData,
        stored: UserProfileResponse,
    ): UpdateUserProfileRequest? {
        val ageChanged = current.age > 0 && current.age != (stored.ageYears ?: 0)
        val heightChanged = current.height > 0 && current.height != (stored.heightCm?.toInt() ?: 0)
        val weightChanged = current.weight > 0 && current.weight != (stored.weightKg?.toInt() ?: 0)
        val experienceChanged = current.experience.isNotBlank() && current.experience != (stored.experience ?: "")
        val frequencyChanged = current.frequency.isNotBlank() && current.frequency != (stored.frequency ?: "")

        val currentHealth = when (current.hasLimitations) {
            AiProfileData.HEALTH_YES -> current.limitationsDesc
            AiProfileData.HEALTH_NO -> ""
            else -> null
        }
        val healthChanged = currentHealth != null && currentHealth != (stored.healthIssues ?: "")

        if (!ageChanged && !heightChanged && !weightChanged &&
            !experienceChanged && !frequencyChanged && !healthChanged
        ) return null

        return UpdateUserProfileRequest(
            ageYears = if (ageChanged) current.age else null,
            heightCm = if (heightChanged) current.height.toDouble() else null,
            weightKg = if (weightChanged) current.weight.toDouble() else null,
            experience = if (experienceChanged) current.experience else null,
            frequency = if (frequencyChanged) current.frequency else null,
            healthIssues = if (healthChanged) currentHealth else null,
        )
    }

    fun refreshProfileFromStore() {
        if (_state.value.step != AiFlowStep.CHOOSE) return
        val stored = userProfileStore.profile.value ?: return
        _state.update { it.copy(profile = stored.toAiProfileData()) }
    }

    fun reset() {
        val stored = userProfileStore.profile.value
        _state.value = if (stored != null) {
            AiUiState(profile = stored.toAiProfileData())
        } else {
            AiUiState()
        }
        scope.launch { aiApi.clearSession() }
    }

    fun onCleared() {
        scope.cancel()
    }

    private companion object {
        val PROFILE_STEPS = setOf(AiFlowStep.PROFILE, AiFlowStep.EXPERIENCE, AiFlowStep.HEALTH)
    }
}

private fun UserProfileResponse.toAiProfileData() = AiProfileData(
    age = ageYears ?: 0,
    height = heightCm?.toInt() ?: 0,
    weight = weightKg?.toInt() ?: 0,
    experience = experience ?: "",
    frequency = frequency ?: "",

    hasLimitations = if (!healthIssues.isNullOrBlank()) AiProfileData.HEALTH_YES else "",
    limitationsDesc = healthIssues ?: "",
)
