import SwiftUI
import Shared

@MainActor
final class AuthViewModelWrapper: ObservableObject {
    private let vm: Shared.AuthViewModel

    @Published private(set) var email: String = ""
    @Published private(set) var password: String = ""
    @Published private(set) var name: String = ""
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var loginSuccess: Bool = false
    @Published private(set) var registerSuccess: Bool = false
    @Published private(set) var subscriptionType: String = "FREE"

    private var observationTask: Task<Void, Never>?

    init() {
        self.vm = Shared.AuthViewModel(
            authApi: KoinHelper.shared.getAuthApi(),
            tokenStorage: KoinHelper.shared.getTokenStorage()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? AuthUiState else { continue }
                self.email = state.email
                self.password = state.password
                self.name = state.name
                self.isLoading = state.isLoading
                self.errorMessage = state.errorMessage
                self.loginSuccess = state.loginSuccess
                self.registerSuccess = state.registerSuccess
                self.subscriptionType = state.subscriptionType

                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func onEmailChanged(_ email: String) {
        vm.onEmailChanged(email: email)
    }

    func onPasswordChanged(_ password: String) {
        vm.onPasswordChanged(password: password)
    }

    func onNameChanged(_ name: String) {
        vm.onNameChanged(name: name)
    }

    func login() {
        vm.login()
    }

    func register() {
        vm.register()
    }

    func consumeLoginSuccess() {
        vm.consumeLoginSuccess()
    }

    func consumeRegisterSuccess() {
        vm.consumeRegisterSuccess()
    }

    func clearFields() {
        vm.resetState()
    }

    func resetState() {
        vm.resetState()
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
