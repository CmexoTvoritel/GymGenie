package com.asc.gymgenie

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.datastore.preferences.preferencesDataStore
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.feature.auth.AuthScreen
import com.asc.gymgenie.feature.main.MainScreen
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.feature.onboarding.OnboardingScreen
import com.asc.gymgenie.feature.paywall.PaywallScreen
import com.asc.gymgenie.feature.paywall.PurchaseSuccessScreen
import com.asc.gymgenie.feature.privacy.PrivacyScreen
import com.asc.gymgenie.presentation.AuthViewModel
import com.asc.gymgenie.ui.theme.GymGenieTheme
import com.asc.gymgenie.user.UserApi
import com.asc.gymgenie.user.UserProfileStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gymgenie_prefs")

private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

sealed class Screen {
    data object Splash : Screen()
    data object Onboarding : Screen()
    data object Privacy : Screen()
    data object Login : Screen()
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

        // Resolved from Koin so every screen that asks for `TokenStorage`,
        // `UserApi` or `UserProfileStore` receives the same singleton wired in
        // `GymGenieApplication.onCreate()`. Resolution happens once per
        // composition root and is cached via `remember`.
        val koin = remember { GlobalContext.get() }
        val tokenStorage = remember { koin.get<TokenStorage>() }
        val userApi = remember { koin.get<UserApi>() }
        val userProfileStore = remember { koin.get<UserProfileStore>() }
        val sessionManager = remember { koin.get<SessionManager>() }

        val authViewModel = remember {
            AuthViewModel(tokenStorage = tokenStorage)
        }

        DisposableEffect(Unit) {
            onDispose {
                authViewModel.onCleared()
            }
        }

        // Network layer fires this when a refresh definitively fails (or
        // there is no refresh token to use). The app forgets any in-memory
        // user state and routes the user back to the login screen so they
        // can sign in again with fresh credentials.
        LaunchedEffect(sessionManager) {
            sessionManager.logoutEvent.collect {
                userProfileStore.clear()
                currentScreen = Screen.Login
            }
        }

        // Read DataStore to determine initial screen
        LaunchedEffect(Unit) {
            val onboardingCompleted = context.dataStore.data
                .map { prefs -> prefs[ONBOARDING_COMPLETED] ?: false }
                .first()

            val hasToken = tokenStorage.getAccessToken() != null

            currentScreen = when {
                hasToken -> Screen.Main
                onboardingCompleted -> Screen.Login
                else -> Screen.Onboarding
            }

            // When the user is already authenticated, eagerly populate the
            // shared profile store so any downstream presenter (AI flow,
            // Profile, ...) can render with real values on first composition.
            if (hasToken) {
                scope.launch { userProfileStore.load() }
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
                AuthScreen(
                    viewModel = authViewModel,
                    initialIsLogin = true,
                    onAuthSuccess = {
                        // Backend is authoritative for premium state. Read it
                        // off the auth response so paid users skip the paywall.
                        val subscriptionType = authViewModel.state.value.subscriptionType
                        currentScreen = if (subscriptionType == "PREMIUM") Screen.Main else Screen.Paywall
                    },
                )
            }

            Screen.Paywall -> {
                PaywallScreen(
                    userApi = userApi,
                    userProfileStore = userProfileStore,
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
                MainScreen(
                    tokenStorage = tokenStorage,
                    userProfileStore = userProfileStore,
                    onLogout = { currentScreen = Screen.Login },
                )
            }
        }
    }
}
