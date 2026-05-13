package com.asc.gymgenie.navigation.tabs.profile

import kotlinx.serialization.Serializable

@Serializable
sealed class ProfileConfig {

    @Serializable
    data object Main : ProfileConfig()

    @Serializable
    data object EditProfile : ProfileConfig()

    @Serializable
    data object EditMetrics : ProfileConfig()

    @Serializable
    data object EditExperience : ProfileConfig()

    @Serializable
    data object EditHealth : ProfileConfig()

    @Serializable
    data object Paywall : ProfileConfig()
}
