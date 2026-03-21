package com.asc.gymgenie.feature.paywall

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PaywallPlan {
    MONTHLY,
    YEARLY,
}

data class PaywallUiState(
    val selectedPlan: PaywallPlan = PaywallPlan.YEARLY,
    val isPurchasing: Boolean = false,
)

class PaywallViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    fun selectPlan(plan: PaywallPlan) {
        _uiState.update { it.copy(selectedPlan = plan) }
    }

    fun purchase(onSuccess: () -> Unit) {
        // TODO:GymGenie - Replace with real purchase logic (StoreKit/BillingClient)
        _uiState.update { it.copy(isPurchasing = true) }
        // Mock immediate success
        onSuccess()
        _uiState.update { it.copy(isPurchasing = false) }
    }
}
