import SwiftUI
import Shared

enum AppScreen {
    case splash
    case onboarding
    case privacy
    case login
    case register
    case paywall
    case purchaseSuccess
    case main
}

final class AppState: ObservableObject {
    @Published var currentScreen: AppScreen = .splash

    private let onboardingCompletedKey = "onboarding_completed"
    private let tokenStorage: TokenStorage
    private let userProfileStore: UserProfileStore
    private var logoutSubscription: SessionSubscription?

    init() {
        self.tokenStorage = KoinHelper.shared.getTokenStorage()
        self.userProfileStore = KoinHelper.shared.getUserProfileStore()
        resolveInitialScreen()
        startObservingLogout()
    }

    deinit {
        logoutSubscription?.cancel()
    }

    /// Single source of truth for forced and explicit logout on iOS.
    ///
    /// Mirrors the Android `App.kt` listener: any emission on
    /// `SessionManager.logoutEvent` clears the in-memory user state (tokens
    /// + profile cache) and routes back to the login screen. Per-wrapper
    /// `isLoggedOut` flags still drive view-local navigation reactions
    /// inside individual screens, but the canonical state reset happens
    /// here so it cannot be missed.
    private func startObservingLogout() {
        let sessionManager = KoinHelper.shared.getSessionManager()
        logoutSubscription = sessionManager.observeLogout { [weak self] in
            guard let self else { return }
            Task { @MainActor in
                try? await self.tokenStorage.clearTokens()
                self.userProfileStore.clear()
                self.navigate(to: .login)
            }
        }
    }

    func navigate(to screen: AppScreen) {
        withAnimation(.easeInOut(duration: 0.3)) {
            currentScreen = screen
        }
    }

    func completeOnboarding() {
        UserDefaults.standard.set(true, forKey: onboardingCompletedKey)
        navigate(to: .login)
    }

    func completePrivacy() {
        UserDefaults.standard.set(true, forKey: onboardingCompletedKey)
        navigate(to: .login)
    }

    /// Routes the user after a successful login/register based on the
    /// authoritative `subscriptionType` returned by the backend. Premium users
    /// skip the paywall; everyone else lands on it.
    func completeLogin(isPremium: Bool = false) {
        navigate(to: isPremium ? .main : .paywall)
    }

    /// Backend has already been told to activate the subscription by the
    /// paywall view model — this only navigates to the success screen.
    func completePurchase() {
        navigate(to: .purchaseSuccess)
    }

    func completeOnboardingFlow() {
        navigate(to: .main)
    }

    private func resolveInitialScreen() {
        let onboardingDone = UserDefaults.standard.bool(forKey: onboardingCompletedKey)
        let hasToken = UserDefaults.standard.string(forKey: "access_token") != nil
        if hasToken {
            currentScreen = .main
        } else if onboardingDone {
            currentScreen = .login
        } else {
            currentScreen = .onboarding
        }
    }
}
