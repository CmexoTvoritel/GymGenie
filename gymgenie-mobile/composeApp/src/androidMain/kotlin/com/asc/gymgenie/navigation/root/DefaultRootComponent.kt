package com.asc.gymgenie.navigation.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.asc.gymgenie.common.SessionManager
import com.asc.gymgenie.common.clearBearerTokens
import com.asc.gymgenie.navigation.main.DefaultMainComponent
import com.asc.gymgenie.navigation.util.componentScope
import com.asc.gymgenie.presentation.AuthViewModel
import com.asc.gymgenie.presentation.CreateWorkoutViewModel
import com.asc.gymgenie.presentation.HomeViewModel
import com.asc.gymgenie.presentation.ProfileViewModel
import com.asc.gymgenie.storage.TokenStorage
import com.asc.gymgenie.user.UserProfileStore
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val tokenStorage: TokenStorage,
    private val userProfileStore: UserProfileStore,
    private val sessionManager: SessionManager,
    private val httpClient: HttpClient,
    private val authViewModel: AuthViewModel,
    private val homeViewModel: HomeViewModel,
    private val onboardingFlagReader: suspend () -> Boolean,
    private val onboardingFlagWriter: suspend (Boolean) -> Unit,
    private val createWorkoutViewModelProvider: () -> CreateWorkoutViewModel,
    private val profileViewModelProvider: () -> ProfileViewModel,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<RootConfig>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = RootConfig.serializer(),
            initialConfiguration = RootConfig.Splash,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private val scope = lifecycle.componentScope()

    init {
        if (stack.value.active.configuration is RootConfig.Splash) {
            scope.launch {
                val onboardingCompleted = onboardingFlagReader()
                val hasToken = tokenStorage.getAccessToken() != null

                val initial: RootConfig = when {
                    hasToken -> RootConfig.Main
                    onboardingCompleted -> RootConfig.Login
                    else -> RootConfig.Onboarding
                }

                if (hasToken) {
                    userProfileStore.load()
                }

                navigation.replaceAll(initial)
            }
        } else {
            scope.launch {
                if (tokenStorage.getAccessToken() != null) {
                    userProfileStore.load()
                }
            }
        }

        scope.launch {
            sessionManager.logoutEvent.collect {
                httpClient.clearBearerTokens()
                tokenStorage.clearTokens()
                userProfileStore.clear()
                authViewModel.resetState()
                homeViewModel.reset()
                navigation.replaceAll(RootConfig.Login)
            }
        }

        lifecycle.doOnDestroy { authViewModel.onCleared() }
    }

    private fun createChild(
        config: RootConfig,
        childContext: ComponentContext,
    ): RootComponent.Child = when (config) {
        RootConfig.Splash -> RootComponent.Child.Splash
        RootConfig.Onboarding -> RootComponent.Child.Onboarding
        RootConfig.Privacy -> RootComponent.Child.Privacy
        RootConfig.Login -> RootComponent.Child.Login(authViewModel)
        RootConfig.Paywall -> RootComponent.Child.Paywall
        RootConfig.PurchaseSuccess -> RootComponent.Child.PurchaseSuccess
        RootConfig.Main -> RootComponent.Child.Main(
            component = DefaultMainComponent(
                componentContext = childContext,
                createWorkoutViewModelProvider = createWorkoutViewModelProvider,
                profileViewModelProvider = profileViewModelProvider,
            ),
        )
    }

    override fun onAuthSuccess() {
        scope.launch { userProfileStore.load() }
        val isPremium = authViewModel.state.value.subscriptionType == "PREMIUM"
        navigation.replaceAll(if (isPremium) RootConfig.Main else RootConfig.Paywall)
    }

    override fun onPaywallPurchaseSuccess() {
        navigation.replaceAll(RootConfig.PurchaseSuccess)
    }

    override fun onPaywallSkipped() {
        navigation.replaceAll(RootConfig.Main)
    }

    override fun onPurchaseSuccessContinue() {
        navigation.replaceAll(RootConfig.Main)
    }

    override fun onOnboardingFinished() {
        navigation.replaceAll(RootConfig.Privacy)
    }

    override fun onPrivacyAccepted() {
        scope.launch {
            onboardingFlagWriter(true)
        }
        navigation.replaceAll(RootConfig.Login)
    }
}
