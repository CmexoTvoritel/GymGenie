import SwiftUI

struct ContentView: View {
    @StateObject private var appState = AppState()
    @StateObject private var authViewModel = AuthViewModelWrapper()

    var body: some View {
        Group {
            switch appState.currentScreen {
            case .splash:
                Color(red: 0.961, green: 0.969, blue: 0.980)
                    .edgesIgnoringSafeArea(.all)

            case .onboarding:
                OnboardingView()

            case .privacy:
                PrivacyView()

            case .login:
                LoginView(viewModel: authViewModel)

            case .register:
                RegisterView(viewModel: authViewModel)

            case .paywall:
                PaywallView()

            case .purchaseSuccess:
                PurchaseSuccessView()

            case .main:
                MainView()
            }
        }
        .environmentObject(appState)
    }
}
