import SwiftUI
import Shared

enum CreateWorkoutStep: Equatable {
    case muscleGroupPicker
    case exercisePicker(muscleGroupKey: String, nameRu: String)
    case exerciseConfig(exercise: Shared.ExerciseShortResponse)

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

struct CreateWorkoutFlowView: View {
    let onDismiss: () -> Void
    let initialExercise: Shared.ExerciseShortResponse?

    @StateObject private var vm = CreateWorkoutViewModelWrapper()
    @State private var stack: [CreateWorkoutStep]
    @State private var showDismissAlert = false

    private let startedFromCatalog: Bool

    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)

    init(onDismiss: @escaping () -> Void, initialExercise: Shared.ExerciseShortResponse? = nil) {
        self.onDismiss = onDismiss
        self.initialExercise = initialExercise
        self.startedFromCatalog = initialExercise != nil
        if let exercise = initialExercise {
            _stack = State(initialValue: [.exerciseConfig(exercise: exercise)])
        } else {
            _stack = State(initialValue: [.muscleGroupPicker])
        }
    }

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
                onBack: {
                    if stack.count <= 1 {
                        requestDismiss()
                    } else {
                        pop()
                    }
                },
                onConfirm: { pending in
                    vm.addExercise(pending)
                    advanceToBuilder()
                },
                showStepHeader: !startedFromCatalog || stack.count > 1
            )

        case let .exerciseEdit(exercise, editingIndex):

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

    private var hasUnsavedData: Bool {
        !vm.workoutName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
        !vm.exercises.isEmpty
    }

    private func requestDismiss() {
        if hasUnsavedData {
            showDismissAlert = true
        } else {
            vm.reset()
            onDismiss()
        }
    }

    private func backFromMuscleGroupPicker() {
        if stack.count == 1 {
            requestDismiss()
        } else {
            pop()
        }
    }

    private func backFromBuilder() {
        requestDismiss()
    }

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
