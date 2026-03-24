package com.asc.gymgenie.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PlanType {
    MONTHLY,
    YEARLY,
}

data class PaywallUiState(
    val selectedPlan: PlanType = PlanType.YEARLY,
    val isPurchasing: Boolean = false,
    val purchaseSuccess: Boolean = false,
)

class PaywallViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    fun selectPlan(plan: PlanType) {
        _state.update { it.copy(selectedPlan = plan) }
    }

    fun purchase() {
        // TODO:GymGenie - Replace with real purchase logic (StoreKit/BillingClient)
        _state.update { it.copy(isPurchasing = true) }
        // Mock immediate success for now
        _state.update { it.copy(isPurchasing = false, purchaseSuccess = true) }
    }

    fun consumePurchaseSuccess() {
        _state.update { it.copy(purchaseSuccess = false) }
    }

    fun onCleared() {
        scope.cancel()
    }
}
