package com.asc.gymgenie

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.arkivanov.decompose.defaultComponentContext
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.navigation.root.DefaultRootComponent
import com.asc.gymgenie.navigation.root.RootComponent
import com.asc.gymgenie.navigation.root.RootContent
import com.asc.gymgenie.presentation.AuthViewModel
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.presentation.HomeViewModel
import com.asc.gymgenie.presentation.ProfileViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.user.UserProfileStore
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.context.GlobalContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gymgenie_prefs")

private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

class MainActivity : ComponentActivity() {

    private lateinit var rootComponent: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setOnExitAnimationListener { it.remove() }

        enableEdgeToEdge()

        val koin = GlobalContext.get()

        rootComponent = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            tokenStorage = koin.get<TokenStorage>(),
            userProfileStore = koin.get<UserProfileStore>(),
            sessionManager = koin.get<SessionManager>(),
            httpClient = koin.get<HttpClient>(),
            authViewModel = koin.get<AuthViewModel>(),
            homeViewModel = koin.get<HomeViewModel>(),
            onboardingFlagReader = {
                applicationContext.dataStore.data
                    .map { prefs -> prefs[ONBOARDING_COMPLETED] ?: false }
                    .first()
            },
            onboardingFlagWriter = { value ->
                applicationContext.dataStore.edit { prefs ->
                    prefs[ONBOARDING_COMPLETED] = value
                }
            },
            createWorkoutViewModelProvider = { koin.get<CreateWorkoutViewModel>() },
            profileViewModelProvider = { koin.get<ProfileViewModel>() },
        )

        setContent {
            RootContent(component = rootComponent)
        }
    }
}
