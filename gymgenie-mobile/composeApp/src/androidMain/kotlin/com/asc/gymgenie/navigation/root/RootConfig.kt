package com.asc.gymgenie.navigation.root

import kotlinx.serialization.Serializable

@Serializable
sealed class RootConfig {

    @Serializable
    data object Splash : RootConfig()

    @Serializable
    data object Onboarding : RootConfig()

    @Serializable
    data object Privacy : RootConfig()

    @Serializable
    data object Login : RootConfig()

    @Serializable
    data object Paywall : RootConfig()

    @Serializable
    data object PurchaseSuccess : RootConfig()

    @Serializable
    data object Main : RootConfig()
}
