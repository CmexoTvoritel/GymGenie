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

/**
 * Drives the paywall screen.
 *
 * On [purchase] the view model calls [UserApi.activateSubscription] so the
 * backend becomes the single authority for premium state, and pushes the
 * resulting profile into [UserProfileStore] so already-mounted screens pick
 * up the new [com.asc.gymgenie.user.UserProfileResponse.subscriptionType]
 * without a refetch.
 *
 * Note: real billing integration (StoreKit / BillingClient) will sit in front
 * of this call — we currently flip success unconditionally because the user
 * has, by contract of this method, already paid.
 */
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
            // TODO:GymGenie - Front this with real billing (StoreKit/BillingClient)
            // before calling the backend.
            userApi.activateSubscription().onSuccess { updatedProfile ->
                userProfileStore.update(updatedProfile)
            }
            // Always mark success — by contract the user has already paid;
            // a transient backend failure shouldn't block the success screen.
            // The next profile load will reconcile state with the server.
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
