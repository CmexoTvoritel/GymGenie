// Required because StackNavigator.push (used by openEditProfile / openPaywall and the
// new openEditMetrics / openEditExperience / openEditHealth methods) is @DelicateDecomposeApi.
// We accept the contract: pushes happen only as a result of explicit user actions, not in
// response to async events, so the "duplicate configuration in stack" risk that motivates
// the marker does not apply here.
@file:OptIn(com.arkivanov.decompose.DelicateDecomposeApi::class)

package com.asc.gymgenie.navigation.tabs.profile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.asc.gymgenie.feature.profile.EditFormHolder
import com.asc.gymgenie.presentation.ProfileViewModel

class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val profileViewModelProvider: () -> ProfileViewModel,
) : ProfileComponent, ComponentContext by componentContext {

    override val profileViewModel: ProfileViewModel by lazy {
        profileViewModelProvider().also { vm ->
            lifecycle.doOnDestroy { vm.onCleared() }
        }
    }

    // Shared form state survives configuration changes and is reused by all edit sub-screens.
    override val editForm: EditFormHolder =
        instanceKeeper.getOrCreate { EditFormHolder() }

    private val navigation = StackNavigation<ProfileConfig>()

    override val stack: Value<ChildStack<*, ProfileComponent.Child>> =
        childStack(
            source = navigation,
            serializer = ProfileConfig.serializer(),
            initialConfiguration = ProfileConfig.Main,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(
        config: ProfileConfig,
        @Suppress("UNUSED_PARAMETER") childContext: ComponentContext,
    ): ProfileComponent.Child = when (config) {
        ProfileConfig.Main -> ProfileComponent.Child.Main
        ProfileConfig.EditProfile -> ProfileComponent.Child.EditProfile
        ProfileConfig.EditMetrics -> ProfileComponent.Child.EditMetrics
        ProfileConfig.EditExperience -> ProfileComponent.Child.EditExperience
        ProfileConfig.EditHealth -> ProfileComponent.Child.EditHealth
        ProfileConfig.Paywall -> ProfileComponent.Child.Paywall
    }

    override fun openEditProfile() {
        navigation.push(ProfileConfig.EditProfile)
    }

    override fun openEditMetrics() {
        navigation.push(ProfileConfig.EditMetrics)
    }

    override fun openEditExperience() {
        navigation.push(ProfileConfig.EditExperience)
    }

    override fun openEditHealth() {
        navigation.push(ProfileConfig.EditHealth)
    }

    override fun openPaywall() {
        navigation.push(ProfileConfig.Paywall)
    }

    override fun pop() {
        navigation.pop()
    }

    override fun resetToMain() {
        navigation.replaceAll(ProfileConfig.Main)
    }
}
