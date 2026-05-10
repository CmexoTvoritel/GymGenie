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

    init() {
        self.tokenStorage = KoinHelper.shared.getTokenStorage()
        resolveInitialScreen()
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
