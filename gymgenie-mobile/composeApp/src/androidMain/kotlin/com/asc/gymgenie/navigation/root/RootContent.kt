package com.asc.gymgenie.navigation.root

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.R
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
import com.asc.gymgenie.ui.theme.WarmOffWhite

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

@Composable
private fun SplashContent() {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(160.dp)
                .clip(CircleShape)
                .background(WarmOffWhite)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_app_icon),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Загружаем данные",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )

            Spacer(modifier = Modifier.width(12.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFFFF5A3C),
                strokeWidth = 2.5.dp,
            )
        }
    }
}
