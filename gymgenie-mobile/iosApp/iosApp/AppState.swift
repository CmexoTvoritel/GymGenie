import SwiftUI

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

    init() {
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

    func completeLogin() {
        navigate(to: .paywall)
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
