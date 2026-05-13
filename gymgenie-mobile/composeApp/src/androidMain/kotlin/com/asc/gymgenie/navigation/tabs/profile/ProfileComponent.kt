package com.asc.gymgenie.navigation.tabs.profile

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.asc.gymgenie.feature.profile.EditFormHolder
import com.asc.gymgenie.presentation.ProfileViewModel

interface ProfileComponent {

    val stack: Value<ChildStack<*, Child>>
    val profileViewModel: ProfileViewModel
    val editForm: EditFormHolder

    fun openEditProfile()
    fun openEditMetrics()
    fun openEditExperience()
    fun openEditHealth()
    fun openPaywall()
    fun pop()
    fun resetToMain()

    sealed class Child {
        data object Main : Child()
        data object EditProfile : Child()
        data object EditMetrics : Child()
        data object EditExperience : Child()
        data object EditHealth : Child()
        data object Paywall : Child()
    }
}
