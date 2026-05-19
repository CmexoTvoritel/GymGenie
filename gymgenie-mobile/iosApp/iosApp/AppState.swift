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

    private func startObservingLogout() {
        let sessionManager = KoinHelper.shared.getSessionManager()
        logoutSubscription = sessionManager.observeLogout { [weak self] in
            guard let self else { return }
            Task { @MainActor in
                let httpClient = KoinHelper.shared.getHttpClient()
                AuthenticatedHttpClientKt.clearHttpClientBearerTokens(client: httpClient)
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

    func completeLogin(isPremium: Bool = false) {
        navigate(to: isPremium ? .main : .paywall)
    }

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
