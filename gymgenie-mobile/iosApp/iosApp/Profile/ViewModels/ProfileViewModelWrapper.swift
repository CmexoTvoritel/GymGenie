import SwiftUI
import Shared

@MainActor
final class ProfileViewModelWrapper: ObservableObject {
    private let vm: Shared.ProfileViewModel

    @Published private(set) var isLoading: Bool = false
    @Published private(set) var error: String? = nil
    @Published private(set) var success: Bool = false

    private var observationTask: Task<Void, Never>?

    init() {
        self.vm = Shared.ProfileViewModel(
            userApi: KoinHelper.shared.getUserApi(),
            userProfileStore: KoinHelper.shared.getUserProfileStore()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self else { break }
                guard let state = self.vm.state.value as? ProfileUpdateState else { continue }

                self.isLoading = state.isLoading
                self.error = state.error
                self.success = state.success

                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func updateProfile(request: UpdateUserProfileRequest) {
        vm.updateProfile(request: request)
    }

    func consumeSuccess() {
        vm.consumeSuccess()
    }

    func clearError() {
        vm.clearError()
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
