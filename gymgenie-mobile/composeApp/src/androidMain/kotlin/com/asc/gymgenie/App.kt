package com.asc.gymgenie

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.gymgenie.feature.auth.AuthViewModel
import com.asc.gymgenie.feature.auth.LoginScreen
import com.asc.gymgenie.feature.auth.RegisterScreen
import com.asc.gymgenie.feature.main.MainScreen
import com.asc.gymgenie.feature.onboarding.OnboardingScreen
import com.asc.gymgenie.feature.paywall.PaywallScreen
import com.asc.gymgenie.feature.paywall.PurchaseSuccessScreen
import com.asc.gymgenie.feature.privacy.PrivacyScreen
import com.asc.gymgenie.ui.theme.GymGenieTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gymgenie_prefs")

private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")

sealed class Screen {
    data object Splash : Screen()
    data object Onboarding : Screen()
    data object Privacy : Screen()
    data object Login : Screen()
    data object Register : Screen()
    data object Paywall : Screen()
    data object PurchaseSuccess : Screen()
    data object Main : Screen()
}

@Composable
fun App() {
    GymGenieTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
        val authViewModel: AuthViewModel = viewModel()

        // Read DataStore to determine initial screen
        LaunchedEffect(Unit) {
            val onboardingCompleted = context.dataStore.data
                .map { prefs -> prefs[ONBOARDING_COMPLETED] ?: false }
                .first()

            val hasToken = context.dataStore.data
                .map { prefs -> prefs[ACCESS_TOKEN_KEY] != null }
                .first()

            currentScreen = when {
                hasToken -> Screen.Main
                onboardingCompleted -> Screen.Login
                else -> Screen.Onboarding
            }
        }

        when (currentScreen) {
            Screen.Splash -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            Screen.Onboarding -> {
                OnboardingScreen(
                    onFinished = {
                        currentScreen = Screen.Privacy
                    },
                )
            }

            Screen.Privacy -> {
                PrivacyScreen(
                    onAccepted = {
                        currentScreen = Screen.Login
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[ONBOARDING_COMPLETED] = true
                            }
                        }
                    },
                )
            }

            Screen.Login -> {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        currentScreen = Screen.Paywall
                    },
                    onNavigateToRegister = {
                        authViewModel.resetState()
                        currentScreen = Screen.Register
                    },
                )
            }

            Screen.Register -> {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        currentScreen = Screen.Paywall
                    },
                    onNavigateToLogin = {
                        authViewModel.resetState()
                        currentScreen = Screen.Login
                    },
                )
            }

            Screen.Paywall -> {
                PaywallScreen(
                    onPurchaseSuccess = {
                        currentScreen = Screen.PurchaseSuccess
                    },
                    onSkip = {
                        currentScreen = Screen.Main
                    },
                )
            }

            Screen.PurchaseSuccess -> {
                PurchaseSuccessScreen(
                    onContinue = {
                        currentScreen = Screen.Main
                    },
                )
            }

            Screen.Main -> {
                MainScreen()
            }
        }
    }
}
