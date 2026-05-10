import SwiftUI
import Shared

@MainActor
final class WorkoutSessionViewModelWrapper: ObservableObject {
    let vm: WorkoutSessionViewModel

    @Published private(set) var phase: WorkoutSessionViewModel.Phase = WorkoutSessionViewModel.Phase.exercise
    @Published private(set) var currentExerciseIndex: Int32 = 0
    @Published private(set) var currentSetIndex: Int32 = 0
    @Published private(set) var restSecondsRemaining: Int32 = 0
    @Published private(set) var currentWeight: Double = 60.0
    @Published private(set) var currentReps: Int32 = 12
    @Published private(set) var isFinished: Bool = false
    @Published private(set) var sessionDurationSeconds: Int32 = 0
    @Published private(set) var currentExercise: ActiveExercise? = nil
    @Published private(set) var nextExercise: ActiveExercise? = nil
    @Published private(set) var totalSets: Int32 = 0
    @Published private(set) var displaySetNumber: Int32 = 1
    @Published private(set) var totalExercises: Int32 = 0
    @Published private(set) var completedSetsCount: Int32 = 0
    @Published private(set) var isSubmitting: Bool = false
    @Published private(set) var isSubmitted: Bool = false
    @Published private(set) var submitError: String? = nil

    private var observationTask: Task<Void, Never>?

    /// - Parameters:
    ///   - session: the active session built by the calling screen.
    ///   - localRepository: SQLDelight-backed offline store. Each completed/skipped
    ///     set is persisted here as it happens; the rows are removed only after a
    ///     successful submit so a transient error does not lose data.
    ///
    /// The wrapper builds an authenticated `HttpClient` internally and feeds it
    /// into `WorkoutApi`, so the shared VM no longer needs a separate access-token
    /// provider — token refresh is handled transparently by the auth interceptor.
    init(
        session: ActiveWorkoutSession,
        localRepository: LocalWorkoutRepository
    ) {
        self.vm = WorkoutSessionViewModel(
            session: session,
            localRepository: localRepository,
            workoutApi: KoinHelper.shared.getWorkoutApi(),
            sessionIdSeed: Self.generateSessionId()
        )
        startObserving()
    }

    private func startObserving() {
        observationTask = Task { [weak self] in
            while !Task.isCancelled {
                guard let self = self else { break }
                guard let state = self.vm.state.value as? WorkoutSessionViewModel.State else {
                    try? await Task.sleep(nanoseconds: 50_000_000)
                    continue
                }
                self.phase = state.phase
                self.currentExerciseIndex = state.currentExerciseIndex
                self.currentSetIndex = state.currentSetIndex
                self.restSecondsRemaining = state.restSecondsRemaining
                self.currentWeight = state.currentWeight
                self.currentReps = state.currentReps
                self.isFinished = state.isFinished
                self.sessionDurationSeconds = state.sessionDurationSeconds
                self.currentExercise = state.currentExercise
                self.nextExercise = state.nextExercise
                self.totalSets = state.totalSets
                self.displaySetNumber = state.displaySetNumber
                self.totalExercises = Int32(state.totalExercises)
                self.completedSetsCount = Int32(state.completedSets.count)
                self.isSubmitting = state.isSubmitting
                self.isSubmitted = state.isSubmitted
                self.submitError = state.submitError
                try? await Task.sleep(nanoseconds: 50_000_000)
            }
        }
    }

    func completeSet() { vm.completeSet() }
    func skipRest() { vm.skipRest() }
    /// Records the current set as skipped (reps=0, weight=0) and advances to the next set/exercise.
    func skipSet() { vm.skipSet() }
    func adjustRest(_ delta: Int32) { vm.adjustRest(deltaSeconds: delta) }
    func adjustWeight(_ delta: Double) { vm.adjustWeight(delta: delta) }
    func adjustReps(_ delta: Int32) { vm.adjustReps(delta: delta) }
    /// Re-attempts the backend submit when a previous attempt failed. The shared
    /// VM guards against parallel/duplicate submissions, so it is safe to wire to
    /// a "retry" UI affordance directly.
    func retrySubmit() { vm.retrySubmit() }

    /// Total volume (kg) lifted across all completed (non-skipped) sets.
    /// Skipped sets are detected by the `repsActual == 0 && weightActual == 0.0` convention.
    func totalVolumeKg() -> Int {
        guard let state = vm.state.value as? WorkoutSessionViewModel.State else { return 0 }
        let volume = state.completedSets.reduce(0.0) { acc, set in
            let reps = Int(set.repsActual)
            let weight = set.weightActual
            if reps == 0 && weight == 0.0 { return acc } // skipped
            return acc + (weight * Double(reps))
        }
        return Int(volume)
    }

    deinit {
        observationTask?.cancel()
        vm.dispose()
    }

    /// Mirrors the format used by the shared VM's internal generator so locally-
    /// scoped session ids stay visually consistent across platforms even when
    /// they originate from Swift.
    private static func generateSessionId() -> String {
        let now = Int(Date().timeIntervalSince1970 * 1000)
        let random = (0..<10).map { _ in
            String(Int.random(in: 0..<36), radix: 36)
        }.joined()
        return "ws-\(now)-\(random)"
    }
}
