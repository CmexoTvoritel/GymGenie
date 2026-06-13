package com.asc.gymgenie.navigation.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.asc.gymgenie.navigation.main.MainComponent
import com.asc.gymgenie.presentation.AuthViewModel

interface RootComponent {

    val stack: Value<ChildStack<*, Child>>

    fun onAuthSuccess()
    fun onPaywallPurchaseSuccess()
    fun onPaywallSkipped()
    fun onPurchaseSuccessContinue()
    fun onOnboardingFinished()
    fun onPrivacyAccepted()

    sealed class Child {
        data object Splash : Child()
        data object Onboarding : Child()
        data object Privacy : Child()

        class Login(val authViewModel: AuthViewModel) : Child()
        data object Paywall : Child()
        data object PurchaseSuccess : Child()
        class Main(val component: MainComponent) : Child()
    }
}
