import SwiftUI
import Shared

/// Steps of the create-workout flow. Enum-based navigation keeps the state
/// machine explicit and testable — each value captures exactly what the
/// receiving view needs.
enum CreateWorkoutStep: Equatable {
    case muscleGroupPicker
    case exercisePicker(muscleGroupKey: String, nameRu: String)
    case exerciseConfig(exercise: Shared.ExerciseShortResponse)
    /// Reuses the config screen as an edit surface for an already-added row.
    /// Captures the index so `vm.updateExercise(at:updated:)` can patch the
    /// correct entry; the `exercise` is rebuilt locally from the stored
    /// `PendingExercise` so we never refetch over the network just to edit
    /// sets/weights.
    case exerciseEdit(exercise: Shared.ExerciseShortResponse, editingIndex: Int)
    case workoutBuilder

    static func == (lhs: CreateWorkoutStep, rhs: CreateWorkoutStep) -> Bool {
        switch (lhs, rhs) {
        case (.muscleGroupPicker, .muscleGroupPicker),
             (.workoutBuilder, .workoutBuilder):
            return true
        case let (.exercisePicker(a1, a2), .exercisePicker(b1, b2)):
            return a1 == b1 && a2 == b2
        case let (.exerciseConfig(a), .exerciseConfig(b)):
            return a.id == b.id
        case let (.exerciseEdit(a, ai), .exerciseEdit(b, bi)):
            return a.id == b.id && ai == bi
        default:
            return false
        }
    }
}

/// Container for the entire create-workout flow.
///
/// Owns the shared `CreateWorkoutViewModelWrapper` and a manual navigation
/// stack. Avoids `NavigationStack` deliberately so back-navigation semantics
/// (e.g. "after configuring an exercise, pop back to the builder, NOT the
/// picker") are explicit and do not fight SwiftUI's automatic handling.
struct CreateWorkoutFlowView: View {
    let onDismiss: () -> Void

    @StateObject private var vm = CreateWorkoutViewModelWrapper()
    @State private var stack: [CreateWorkoutStep] = [.muscleGroupPicker]
    @State private var showDismissAlert = false

    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)

    var body: some View {
        ZStack {
            currentStep
                .transition(.opacity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(warmOffWhite.ignoresSafeArea())
        .onAppear {
            vm.loadMuscleGroups()
        }
        .onChange(of: vm.isSaved) { saved in
            if saved {
                // Release shared state before dismissing so the next entry starts clean.
                vm.reset()
                onDismiss()
            }
        }
        .alert(
            "Завершить создание?",
            isPresented: $showDismissAlert,
            actions: {
                Button("Да", role: .destructive) {
                    vm.reset()
                    onDismiss()
                }
                Button("Нет", role: .cancel) { }
            },
            message: {
                Text("Вы уверены, что хотите закончить создание тренировки?")
            }
        )
        .interactiveDismissDisabled(true)
    }

    // MARK: - Step rendering

    @ViewBuilder
    private var currentStep: some View {
        switch stack.last ?? .muscleGroupPicker {
        case .muscleGroupPicker:
            MuscleGroupPickerView(
                vm: vm,
                onBack: { backFromMuscleGroupPicker() },
                onGroupSelected: { key, nameRu in
                    push(.exercisePicker(muscleGroupKey: key, nameRu: nameRu))
                }
            )

        case let .exercisePicker(key, nameRu):
            ExercisePickerView(
                muscleGroupKey: key,
                muscleGroupNameRu: nameRu,
                onBack: { pop() },
                onExerciseSelected: { exercise in
                    push(.exerciseConfig(exercise: exercise))
                }
            )

        case let .exerciseConfig(exercise):
            ExerciseConfigView(
                exercise: exercise,
                onBack: { pop() },
                onConfirm: { pending in
                    vm.addExercise(pending)
                    advanceToBuilder()
                }
            )

        case let .exerciseEdit(exercise, editingIndex):
            // Snapshot the row by index from the live wrapper. If it was
            // removed underneath us (stale index), pop instead of rendering
            // a half-broken edit screen.
            if vm.exercises.indices.contains(editingIndex) {
                ExerciseConfigView(
                    exercise: exercise,
                    onBack: { pop() },
                    onConfirm: { updated in
                        vm.updateExercise(at: editingIndex, updated: updated)
                        pop()
                    },
                    prefillFrom: vm.exercises[editingIndex],
                    showStepHeader: false
                )
            } else {
                // SwiftUI requires the branch to return a view; an empty
                // placeholder is fine because the onAppear pops immediately.
                Color.clear.onAppear { pop() }
            }

        case .workoutBuilder:
            WorkoutBuilderView(
                vm: vm,
                onBack: { backFromBuilder() },
                onAddExercise: {
                    push(.muscleGroupPicker)
                },
                onEditExercise: { index, pending in
                    // Rebuild a minimal [ExerciseShortResponse] from the
                    // [PendingExercise] — only id / names / muscleGroup /
                    // requiresWeight are read by the edit surface.
                    let exerciseShort = Shared.ExerciseShortResponse(
                        id: pending.exerciseId,
                        nameRu: pending.exerciseNameRu,
                        nameEn: pending.exerciseNameEn,
                        muscleGroup: pending.muscleGroupKey,
                        category: "",
                        difficultyLevel: "",
                        secondsPer10Reps: nil,
                        caloriesBurned: nil,
                        rating: nil,
                        imageUrl: nil,
                        requiresWeight: pending.requiresWeight
                    )
                    push(.exerciseEdit(exercise: exerciseShort, editingIndex: index))
                }
            )
        }
    }

    // MARK: - Navigation

    private func push(_ step: CreateWorkoutStep) {
        withAnimation(.easeInOut(duration: 0.15)) {
            stack.append(step)
        }
    }

    private func pop() {
        guard stack.count > 1 else { return }
        withAnimation(.easeInOut(duration: 0.15)) {
            _ = stack.popLast()
        }
    }

    /// Returns `true` when the user has entered any meaningful data that would
    /// be lost if the flow were dismissed without saving.
    private var hasUnsavedData: Bool {
        !vm.workoutName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
        !vm.exercises.isEmpty
    }

    /// Dismisses the flow immediately if nothing to lose, or shows the
    /// confirmation alert otherwise.
    private func requestDismiss() {
        if hasUnsavedData {
            showDismissAlert = true
        } else {
            vm.reset()
            onDismiss()
        }
    }

    /// Pressing "back" on the first muscle-group picker exits the whole flow.
    /// On subsequent visits (after the builder is already in the stack) it
    /// simply pops back to the builder without losing state.
    private func backFromMuscleGroupPicker() {
        if stack.count == 1 {
            requestDismiss()
        } else {
            pop()
        }
    }

    /// The builder is the "home" of the flow. Backing out of it cancels the
    /// entire workout draft.
    private func backFromBuilder() {
        requestDismiss()
    }

    /// After confirming an exercise, make sure the builder is the top of the
    /// stack. If the user already had a builder behind them we pop back to
    /// it; otherwise we establish it as the destination.
    private func advanceToBuilder() {
        if let existingIndex = stack.firstIndex(of: .workoutBuilder) {
            withAnimation(.easeInOut(duration: 0.15)) {
                stack = Array(stack.prefix(through: existingIndex))
            }
        } else {
            withAnimation(.easeInOut(duration: 0.15)) {
                stack = [.workoutBuilder]
            }
        }
    }
}
