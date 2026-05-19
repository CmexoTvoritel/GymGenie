package com.asc.gymgenie.navigation.tabs.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.asc.gymgenie.feature.ai.AiFlowScreen
import com.asc.gymgenie.ui.components.PremiumLockedOverlay
import com.asc.gymgenie.user.UserProfileStore
import org.koin.core.context.GlobalContext

@Composable
fun AiContent(
    component: AiComponent,
    onOpenPaywall: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val koin = remember { GlobalContext.get() }
    val profileStore = remember { koin.get<UserProfileStore>() }
    val profile by profileStore.profile.collectAsState()
    val isPremium = profile?.subscriptionType?.let { it != "FREE" } ?: false

    if (isPremium) {
        AiFlowScreen(onBottomBarVisibilityChanged = component::setBottomBarVisible)
    } else {
        PremiumLockedOverlay(onUnlock = onOpenPaywall)
    }
}
