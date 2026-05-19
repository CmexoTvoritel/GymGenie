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
    @Published private(set) var workoutDescription: String = ""
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
        self.vm = Shared.CreateWorkoutViewModel(
            exerciseApi: KoinHelper.shared.getExerciseApi(),
            workoutApi: KoinHelper.shared.getWorkoutApi()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? Shared.CreateWorkoutUiState else { continue }
                self.workoutName = state.workoutName
                self.workoutDescription = state.description_
                self.restSeconds = state.restSeconds
                self.exercises = state.exercises as [Shared.PendingExercise]
                self.muscleGroups = state.muscleGroups as [Shared.MuscleGroupInfo]
                self.isMuscleGroupsLoading = state.isMuscleGroupsLoading
                self.isMuscleGroupsLoaded = state.isMuscleGroupsLoaded
                self.isSaving = state.isSaving
                self.isSaved = state.isSaved
                self.errorMessage = state.errorMessage
                self.scheduleType = state.scheduleType
                self.scheduleDays = state.scheduleDays as Set<String>

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

    func setDescription(_ description: String) {
        vm.setDescription(description: description)
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

    func updateExercise(at index: Int, updated: Shared.PendingExercise) {
        vm.updateExerciseAt(index: Int32(index), updated: updated)
    }

    func moveExercise(from: Int, to: Int) {
        guard from != to,
              exercises.indices.contains(from),
              exercises.indices.contains(to)
        else { return }
        var reordered = exercises
        let item = reordered.remove(at: from)
        reordered.insert(item, at: to)
        for (i, exercise) in reordered.enumerated() {
            vm.updateExerciseAt(index: Int32(i), updated: exercise)
        }
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
