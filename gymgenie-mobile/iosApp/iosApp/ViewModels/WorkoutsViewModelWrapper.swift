import SwiftUI
import Shared

@MainActor
final class WorkoutsViewModelWrapper: ObservableObject {
    private let vm: Shared.WorkoutsViewModel

    @Published private(set) var selectedTab: Shared.WorkoutsTab = .workouts
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var isLoadingMore: Bool = false
    @Published private(set) var workoutPlans: [WorkoutPlanShortResponse] = []
    @Published private(set) var workoutPlansLoaded: Bool = false
    @Published private(set) var exercises: [ExerciseShortResponse] = []
    @Published private(set) var exercisesLoaded: Bool = false
    @Published private(set) var searchQuery: String = ""
    @Published private(set) var selectedMuscleGroup: String? = nil
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var hasMoreExercises: Bool = true
    @Published private(set) var isLoggedOut: Bool = false

    private var observationTask: Task<Void, Never>?

    init() {
        let tokenStorage = TokenStorageKt.createTokenStorage()
        let authApi = AuthApi()
        let client = AuthenticatedHttpClientKt.createAuthenticatedClient(
            tokenStorage: tokenStorage,
            authApi: authApi
        )
        self.vm = Shared.WorkoutsViewModel(
            workoutApi: WorkoutApi(client: client),
            exerciseApi: ExerciseApi(client: client),
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
                guard let state = self.vm.state.value as? WorkoutsUiState else { continue }
                self.selectedTab = state.selectedTab
                self.isLoading = state.isLoading
                self.isLoadingMore = state.isLoadingMore
                self.workoutPlans = state.workoutPlans as? [WorkoutPlanShortResponse] ?? []
                self.workoutPlansLoaded = state.workoutPlansLoaded
                self.exercises = state.exercises as? [ExerciseShortResponse] ?? []
                self.exercisesLoaded = state.exercisesLoaded
                self.searchQuery = state.searchQuery
                self.selectedMuscleGroup = state.selectedMuscleGroup
                self.errorMessage = state.errorMessage
                self.hasMoreExercises = state.hasMoreExercises

                try? await Task.sleep(nanoseconds: 50_000_000) // 50ms
            }
        }
    }

    func loadWorkoutPlans() {
        vm.loadWorkoutPlans()
    }

    func selectTab(_ tab: Shared.WorkoutsTab) {
        vm.selectTab(tab: tab)
    }

    func updateSearchQuery(_ query: String) {
        vm.onSearchQueryChanged(query: query)
    }

    func filterByMuscleGroup(_ group: String?) {
        vm.filterByMuscleGroup(group: group)
    }

    func loadExercises(reset: Bool = false) {
        vm.loadExercises(reset: reset)
    }

    func loadMoreExercises() {
        vm.loadMoreExercises()
    }

    func searchExercises() {
        vm.searchExercises()
    }

    func retry() {
        vm.retry()
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
