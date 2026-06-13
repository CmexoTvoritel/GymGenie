import SwiftUI
import Shared

@MainActor
final class WorkoutsViewModelWrapper: ObservableObject {
    private let vm: Shared.WorkoutsViewModel

    @Published private(set) var selectedTab: Shared.WorkoutsTab = .workouts
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var isLoadingMore: Bool = false
    @Published private(set) var isRefreshing: Bool = false
    @Published private(set) var workoutPlans: [WorkoutPlanShortResponse] = []
    @Published private(set) var workoutPlansLoaded: Bool = false
    @Published private(set) var exercises: [ExerciseShortResponse] = []
    @Published private(set) var exercisesLoaded: Bool = false
    @Published private(set) var searchQuery: String = ""
    @Published private(set) var selectedMuscleGroup: String? = nil
    @Published private(set) var selectedDifficulties: [String] = []
    @Published private(set) var requiresEquipment: Bool? = nil
    @Published private(set) var sortByDifficulty: String? = nil
    @Published private(set) var sortByCalories: String? = nil
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var hasMoreExercises: Bool = true
    @Published private(set) var isLoggedOut: Bool = false
    @Published private(set) var isLoadingSession: Bool = false
    @Published private(set) var pendingSession: ActiveWorkoutSession? = nil
    @Published private(set) var sessionError: String? = nil

    private var observationTask: Task<Void, Never>?
    private var logoutSubscription: SessionSubscription?

    init() {
        self.vm = Shared.WorkoutsViewModel(
            workoutApi: KoinHelper.shared.getWorkoutApi(),
            exerciseApi: KoinHelper.shared.getExerciseApi(),
            tokenStorage: KoinHelper.shared.getTokenStorage(),
            sessionManager: KoinHelper.shared.getSessionManager()
        )
        startObserving()
        let sessionManager = KoinHelper.shared.getSessionManager()
        logoutSubscription = sessionManager.observeLogout { [weak self] in
            Task { @MainActor in self?.isLoggedOut = true }
        }
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? WorkoutsUiState else { continue }
                self.selectedTab = state.selectedTab
                self.isLoading = state.isLoading
                self.isLoadingMore = state.isLoadingMore
                self.isRefreshing = state.isRefreshing
                self.workoutPlans = state.workoutPlans as [WorkoutPlanShortResponse]
                self.workoutPlansLoaded = state.workoutPlansLoaded
                self.exercises = state.exercises as [ExerciseShortResponse]
                self.exercisesLoaded = state.exercisesLoaded
                self.searchQuery = state.searchQuery
                self.selectedMuscleGroup = state.selectedMuscleGroup
                self.selectedDifficulties = state.selectedDifficulties as [String]

                self.requiresEquipment = state.requiresEquipment?.boolValue
                self.sortByDifficulty = state.sortByDifficulty
                self.sortByCalories = state.sortByCalories
                self.errorMessage = state.errorMessage
                self.hasMoreExercises = state.hasMoreExercises
                self.isLoadingSession = state.isLoadingSession
                self.pendingSession = state.pendingSession
                self.sessionError = state.sessionError

                try? await Task.sleep(nanoseconds: 50_000_000)
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

    func applyFilters(difficulties: [String], requiresEquipment: Bool?, sortByDifficulty: String?, sortByCalories: String?) {
        vm.applyFilters(
            difficulties: difficulties,
            requiresEquipment: requiresEquipment.map { KotlinBoolean(bool: $0) },
            sortByDifficulty: sortByDifficulty,
            sortByCalories: sortByCalories
        )
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

    func refresh() {
        vm.refresh()
    }

    func startWorkout(planId: String, planName: String) {
        vm.startWorkout(planId: planId, planName: planName)
    }

    func clearPendingSession() {
        vm.clearPendingSession()
    }

    deinit {
        observationTask?.cancel()
        logoutSubscription?.cancel()
        vm.onCleared()
    }
}
