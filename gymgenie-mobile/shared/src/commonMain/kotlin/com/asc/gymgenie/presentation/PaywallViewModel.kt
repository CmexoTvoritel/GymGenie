package com.asc.gymgenie.presentation

import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PlanType {
    MONTHLY,
    YEARLY,
}

data class PaywallUiState(
    val selectedPlan: PlanType = PlanType.YEARLY,
    val isPurchasing: Boolean = false,
    val purchaseSuccess: Boolean = false,
)

class PaywallViewModel(
    private val userApi: UserApi,
    private val userProfileStore: UserProfileStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    fun selectPlan(plan: PlanType) {
        _state.update { it.copy(selectedPlan = plan) }
    }

    fun purchase() {
        if (_state.value.isPurchasing) return
        _state.update { it.copy(isPurchasing = true) }
        scope.launch {

            userApi.activateSubscription().onSuccess { updatedProfile ->
                userProfileStore.update(updatedProfile)
            }

            _state.update { it.copy(isPurchasing = false, purchaseSuccess = true) }
        }
    }

    fun consumePurchaseSuccess() {
        _state.update { it.copy(purchaseSuccess = false) }
    }

    fun onCleared() {
        scope.cancel()
    }
}
