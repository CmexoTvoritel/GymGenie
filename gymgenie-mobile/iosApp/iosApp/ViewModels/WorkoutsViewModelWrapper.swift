import SwiftUI
import Shared

@MainActor
final class WorkoutsViewModelWrapper: ObservableObject {
    private let vm: Shared.WorkoutsViewModel

    @Published private(set) var selectedTab: Shared.WorkoutsTab = .workouts
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var isLoadingMore: Bool = false
    @Published private(set) var workoutPlans: [WorkoutPlanShortResponse] = []
    @Published private(set) var exercises: [ExerciseShortResponse] = []
    @Published private(set) var searchQuery: String = ""
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var hasMoreExercises: Bool = true

    private var observationTask: Task<Void, Never>?

    init() {
        let tokenStorage = TokenStorageKt.createTokenStorage()
        self.vm = Shared.WorkoutsViewModel(
            workoutApi: WorkoutApi(),
            exerciseApi: ExerciseApi(),
            tokenStorage: tokenStorage
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
                self.exercises = state.exercises as? [ExerciseShortResponse] ?? []
                self.searchQuery = state.searchQuery
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
