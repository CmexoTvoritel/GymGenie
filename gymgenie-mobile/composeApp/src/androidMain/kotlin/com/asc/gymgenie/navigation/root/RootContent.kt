package com.asc.gymgenie.navigation.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.asc.gymgenie.feature.auth.AuthScreen
import com.asc.gymgenie.feature.onboarding.OnboardingScreen
import com.asc.gymgenie.feature.paywall.PaywallScreen
import com.asc.gymgenie.feature.paywall.PurchaseSuccessScreen
import com.asc.gymgenie.feature.privacy.PrivacyScreen
import com.asc.gymgenie.navigation.main.MainContent
import com.asc.gymgenie.ui.theme.GymGenieTheme

@Composable
fun RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier,
) {
    GymGenieTheme {
        Children(
            stack = component.stack,
            modifier = modifier.fillMaxSize(),
            animation = stackAnimation(slide()),
        ) { child ->
            when (val instance = child.instance) {
                RootComponent.Child.Splash -> SplashContent()

                RootComponent.Child.Onboarding -> OnboardingScreen(
                    onFinished = component::onOnboardingFinished,
                )

                RootComponent.Child.Privacy -> PrivacyScreen(
                    onAccepted = component::onPrivacyAccepted,
                )

                is RootComponent.Child.Login -> AuthScreen(
                    viewModel = instance.authViewModel,
                    initialIsLogin = true,
                    onAuthSuccess = component::onAuthSuccess,
                )

                RootComponent.Child.Paywall -> PaywallScreen(
                    onPurchaseSuccess = component::onPaywallPurchaseSuccess,
                    onSkip = component::onPaywallSkipped,
                )

                RootComponent.Child.PurchaseSuccess -> PurchaseSuccessScreen(
                    onContinue = component::onPurchaseSuccessContinue,
                )

                is RootComponent.Child.Main -> MainContent(
                    component = instance.component,
                )
            }
        }
    }
}

/**
 * Branded launch screen rendered while the app resolves its initial
 * destination (DataStore read for onboarding flag, token validation, and
 * profile fetch). The composable is intentionally stateless — navigation is
 * driven entirely by [DefaultRootComponent] and only the visuals live here.
 *
 * Layout: centered logo badge with title and subtitle, with a spinner
 * pinned to the lower portion of the screen so it does not visually compete
 * with the brand mark.
 *
 * The logo is currently a styled "G" placeholder; swap [SplashLogo] for an
 * `Image(painterResource(...))` once a dedicated asset is available.
 */
@Composable
private fun SplashContent() {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SplashLogo()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "GymGenie",
                style = typography.headlineLarge,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Загружаем ваши данные...",
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .size(32.dp),
            color = colors.primary,
            strokeWidth = 3.dp,
        )
    }
}

/**
 * Placeholder logo badge. Renders the brand initial inside a tinted circle
 * so the splash has visual presence even before a vector/raster asset is
 * provided. Replace with the final brand mark when available.
 */
@Composable
private fun SplashLogo() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "G",
            color = colors.onPrimary,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
