import SwiftUI
import Shared

/// SwiftUI-side bridge over the shared `CreateWorkoutViewModel`.
///
/// Mirrors the same 50ms polling pattern used by `WorkoutsViewModelWrapper`
/// so the shared ViewModel remains the single source of truth while SwiftUI
/// gets idiomatic `@Published` values.
@MainActor
final class CreateWorkoutViewModelWrapper: ObservableObject {
    private let vm: Shared.CreateWorkoutViewModel

    @Published private(set) var workoutName: String = ""
    @Published private(set) var restSeconds: Int32 = Int32(Shared.CreateWorkoutLimits.shared.DEFAULT_REST_SECONDS)
    @Published private(set) var exercises: [Shared.PendingExercise] = []
    @Published private(set) var muscleGroups: [Shared.MuscleGroupInfo] = []
    @Published private(set) var isMuscleGroupsLoading: Bool = false
    @Published private(set) var isMuscleGroupsLoaded: Bool = false
    @Published private(set) var isSaving: Bool = false
    @Published private(set) var isSaved: Bool = false
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var scheduleType: Shared.WorkoutScheduleType = .oneTime
    @Published private(set) var scheduleDays: Set<String> = []

    private var observationTask: Task<Void, Never>?

    init() {
        let tokenStorage = TokenStorageKt.createTokenStorage()
        let authApi = AuthApi()
        let client = AuthenticatedHttpClientKt.createAuthenticatedClient(
            tokenStorage: tokenStorage,
            authApi: authApi
        )
        self.vm = Shared.CreateWorkoutViewModel(
            exerciseApi: Shared.ExerciseApi(client: client),
            workoutApi: Shared.WorkoutApi(client: client)
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? Shared.CreateWorkoutUiState else { continue }
                self.workoutName = state.workoutName
                self.restSeconds = state.restSeconds
                self.exercises = state.exercises as? [Shared.PendingExercise] ?? []
                self.muscleGroups = state.muscleGroups as? [Shared.MuscleGroupInfo] ?? []
                self.isMuscleGroupsLoading = state.isMuscleGroupsLoading
                self.isMuscleGroupsLoaded = state.isMuscleGroupsLoaded
                self.isSaving = state.isSaving
                self.isSaved = state.isSaved
                self.errorMessage = state.errorMessage
                self.scheduleType = state.scheduleType
                // Kotlin's `Set<String>` is bridged to Swift as `Set<AnyHashable>`;
                // cast each element back to `String` so the view can compare with
                // its own day-key constants without re-bridging on every read.
                if let days = state.scheduleDays as? Set<String> {
                    self.scheduleDays = days
                } else {
                    self.scheduleDays = Set(
                        (state.scheduleDays as? Set<AnyHashable> ?? []).compactMap { $0 as? String }
                    )
                }

                try? await Task.sleep(nanoseconds: 50_000_000) // 50ms
            }
        }
    }

    // MARK: - Commands forwarded to the shared ViewModel

    func loadMuscleGroups(forceReload: Bool = false) {
        vm.loadMuscleGroups(forceReload: forceReload)
    }

    func setWorkoutName(_ name: String) {
        vm.setWorkoutName(name: name)
    }

    func incrementRestSeconds() {
        vm.incrementRestSeconds()
    }

    func decrementRestSeconds() {
        vm.decrementRestSeconds()
    }

    func addExercise(_ exercise: Shared.PendingExercise) {
        vm.addExercise(exercise: exercise)
    }

    func removeExercise(at index: Int) {
        vm.removeExerciseAt(index: Int32(index))
    }

    func saveWorkout() {
        vm.saveWorkout()
    }

    func setScheduleType(_ type: Shared.WorkoutScheduleType) {
        vm.setScheduleType(type: type)
    }

    func toggleScheduleDay(_ day: String) {
        vm.toggleScheduleDay(day: day)
    }

    func reset() {
        vm.reset()
    }

    func clearError() {
        vm.clearError()
    }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
