import SwiftUI

struct ContentView: View {
    @StateObject private var appState = AppState()
    @StateObject private var authViewModel = AuthViewModelWrapper()

    var body: some View {
        Group {
            switch appState.currentScreen {
            case .splash:
                SplashView()

            case .onboarding:
                OnboardingView()

            case .privacy:
                PrivacyView()

            case .login:
                AuthView(viewModel: authViewModel, initialIsLogin: true)

            case .register:
                AuthView(viewModel: authViewModel, initialIsLogin: false)

            case .paywall:
                PaywallView()

            case .purchaseSuccess:
                PurchaseSuccessView()

            case .main:
                MainView()
            }
        }
        .environmentObject(appState)
        .onChange(of: appState.currentScreen == .login) { isLogin in
            if isLogin {
                authViewModel.resetState()
            }
        }
    }
}
