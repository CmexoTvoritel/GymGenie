import SwiftUI

enum AppScreen {
    case splash
    case onboarding
    case privacy
    case login
    case register
    case home
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
        navigate(to: .home)
    }

    private func resolveInitialScreen() {
        let onboardingDone = UserDefaults.standard.bool(forKey: onboardingCompletedKey)
        if onboardingDone {
            currentScreen = .login
        } else {
            currentScreen = .onboarding
        }
    }
}
