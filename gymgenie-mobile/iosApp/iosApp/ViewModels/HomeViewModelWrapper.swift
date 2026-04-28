import SwiftUI
import Shared

@MainActor
final class HomeViewModelWrapper: ObservableObject {
    private let vm: Shared.HomeViewModel

    @Published private(set) var isLoading: Bool = false
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var username: String = ""
    @Published private(set) var subscriptionType: String = "FREE"
    @Published private(set) var streakDays: Int32 = 0
    @Published private(set) var activeWorkoutPlans: [WorkoutPlanShortResponse] = []
    @Published private(set) var userProfile: UserProfileResponse? = nil
    @Published private(set) var isLoggedOut: Bool = false

    private var observationTask: Task<Void, Never>?

    init() {
        let tokenStorage = TokenStorageKt.createTokenStorage()
        let authApi = AuthApi()
        let client = AuthenticatedHttpClientKt.createAuthenticatedClient(
            tokenStorage: tokenStorage,
            authApi: authApi
        )
        self.vm = Shared.HomeViewModel(
            userApi: UserApi(client: client),
            workoutApi: WorkoutApi(client: client),
            tokenStorage: tokenStorage,
            onLogout: { [weak self] in
                Task { @MainActor in
                    self?.isLoggedOut = true
                }
            }
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? HomeUiState else { continue }
                self.isLoading = state.isLoading
                self.errorMessage = state.errorMessage
                self.username = state.username
                self.subscriptionType = state.subscriptionType
                self.streakDays = state.streakDays
                self.activeWorkoutPlans = state.activeWorkoutPlans as? [WorkoutPlanShortResponse] ?? []
                self.userProfile = state.userProfile

                try? await Task.sleep(nanoseconds: 50_000_000) // 50ms
            }
        }
    }

    func loadData() {
        vm.load()
    }

    func retry() {
        vm.retry()
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
