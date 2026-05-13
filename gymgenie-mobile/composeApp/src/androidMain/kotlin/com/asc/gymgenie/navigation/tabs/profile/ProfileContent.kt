package com.asc.gymgenie.navigation.tabs.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.asc.gymgenie.feature.paywall.PaywallScreen
import com.asc.gymgenie.feature.profile.EditExperienceScreen
import com.asc.gymgenie.feature.profile.EditHealthScreen
import com.asc.gymgenie.feature.profile.EditMetricsScreen
import com.asc.gymgenie.feature.profile.EditProfileScreen
import com.asc.gymgenie.feature.profile.ProfileScreen
import com.asc.gymgenie.ui.theme.WarmOffWhite

@Composable
fun ProfileContent(
    component: ProfileComponent,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(WarmOffWhite)) {
        Children(
            stack = component.stack,
            animation = stackAnimation(slide()),
        ) { child ->
            when (child.instance) {
                ProfileComponent.Child.Main -> ProfileScreen(
                    onOpenEditProfile = component::openEditProfile,
                    onOpenPaywall = component::openPaywall,
                )

                ProfileComponent.Child.EditProfile -> EditProfileScreen(
                    profileViewModel = component.profileViewModel,
                    form = component.editForm,
                    onOpenMetrics = component::openEditMetrics,
                    onOpenExperience = component::openEditExperience,
                    onOpenHealth = component::openEditHealth,
                    onBack = component::pop,
                )

                ProfileComponent.Child.EditMetrics -> EditMetricsScreen(
                    form = component.editForm,
                    onBack = component::pop,
                )

                ProfileComponent.Child.EditExperience -> EditExperienceScreen(
                    form = component.editForm,
                    onBack = component::pop,
                )

                ProfileComponent.Child.EditHealth -> EditHealthScreen(
                    form = component.editForm,
                    onBack = component::pop,
                )

                ProfileComponent.Child.Paywall -> PaywallScreen(
                    onPurchaseSuccess = component::pop,
                    onSkip = component::pop,
                )
            }
        }
    }
}
