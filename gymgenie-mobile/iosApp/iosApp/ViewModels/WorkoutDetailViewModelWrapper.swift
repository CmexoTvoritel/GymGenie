import SwiftUI
import Shared

/// SwiftUI-side bridge over the shared `WorkoutDetailViewModel`.
///
/// Mirrors the 50ms polling pattern used by the other wrappers so the shared
/// ViewModel remains the single source of truth while SwiftUI gets idiomatic
/// `@Published` values to drive bindings.
///
/// The wrapper takes a `planId` at construction time because the shared VM is
/// keyed on it and triggers an initial load in its own `init`. Re-entering the
/// detail screen for a different plan should therefore mean a new wrapper
/// instance (e.g. via SwiftUI's `id(_:)` modifier or a fresh `fullScreenCover`).
@MainActor
final class WorkoutDetailViewModelWrapper: ObservableObject {
    private let vm: Shared.WorkoutDetailViewModel

    // View-mode projection
    @Published private(set) var plan: Shared.WorkoutPlanResponse? = nil
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var isEditing: Bool = false
    @Published private(set) var isSaving: Bool = false
    @Published private(set) var isSaved: Bool = false
    @Published private(set) var isDeleting: Bool = false
    @Published private(set) var isDeleted: Bool = false
    @Published private(set) var errorMessage: String? = nil

    // Edit-mode draft
    @Published private(set) var editName: String = ""
    @Published private(set) var editDescription: String = ""
    @Published private(set) var editRestSeconds: Int32 = Int32(Shared.CreateWorkoutLimits.shared.DEFAULT_REST_SECONDS)
    @Published private(set) var editScheduleType: Shared.WorkoutScheduleType = .oneTime
    @Published private(set) var editScheduleDays: Set<String> = []
    @Published private(set) var editExercises: [Shared.PendingExercise] = []

    private var observationTask: Task<Void, Never>?

    init(planId: String) {
        self.vm = Shared.WorkoutDetailViewModel(
            planId: planId,
            workoutApi: KoinHelper.shared.getWorkoutApi(),
            tokenStorage: KoinHelper.shared.getTokenStorage(),
            sessionManager: KoinHelper.shared.getSessionManager()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? Shared.WorkoutDetailUiState else { continue }

                self.plan = state.plan
                self.isLoading = state.isLoading
                self.isEditing = state.isEditing
                self.isSaving = state.isSaving
                self.isSaved = state.isSaved
                self.isDeleting = state.isDeleting
                self.isDeleted = state.isDeleted
                self.errorMessage = state.errorMessage

                self.editName = state.editName
                self.editDescription = state.editDescription
                self.editRestSeconds = state.editRestSeconds
                self.editScheduleType = state.editScheduleType

                self.editScheduleDays = state.editScheduleDays as Set<String>
                self.editExercises = state.editExercises as [Shared.PendingExercise]

                try? await Task.sleep(nanoseconds: 50_000_000) // 50ms
            }
        }
    }

    // MARK: - Commands forwarded to the shared ViewModel

    func load() { vm.load() }
    func retry() { vm.retry() }

    func startEditing() { vm.startEditing() }
    func cancelEditing() { vm.cancelEditing() }
    func saveEdit() { vm.saveEdit() }

    func setEditName(_ name: String) {
        vm.setEditName(name: name)
    }

    func setEditDescription(_ description: String) {
        vm.setEditDescription(description: description)
    }

    func setEditScheduleType(_ type: Shared.WorkoutScheduleType) {
        vm.setEditScheduleType(type: type)
    }

    func toggleEditScheduleDay(_ day: String) {
        vm.toggleEditScheduleDay(day: day)
    }

    func incrementEditRestSeconds() {
        vm.incrementEditRestSeconds()
    }

    func decrementEditRestSeconds() {
        vm.decrementEditRestSeconds()
    }

    func removeExercise(at index: Int) {
        vm.removeExercise(index: Int32(index))
    }

    func moveExercise(from: Int, to: Int) {
        vm.moveExercise(fromIndex: Int32(from), toIndex: Int32(to))
    }

    func updatePendingExerciseAt(index: Int, updated: Shared.PendingExercise) {
        vm.updatePendingExerciseAt(index: Int32(index), updated: updated)
    }

    func deletePlan() { vm.deletePlan() }

    func consumeSavedFlag() { vm.consumeSavedFlag() }
    func clearError() { vm.clearError() }

    deinit {
        observationTask?.cancel()
        vm.onCleared()
    }
}
