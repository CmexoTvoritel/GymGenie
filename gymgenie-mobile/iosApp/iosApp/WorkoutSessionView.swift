import SwiftUI
import Shared

struct WorkoutSessionView: View {
    @StateObject private var sessionVM: WorkoutSessionViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    @State private var showExitAlert: Bool = false
    @State private var showExerciseInfo: Bool = false

    init(session: ActiveWorkoutSession) {
        let localRepository = KoinHelper.shared.getLocalWorkoutRepository()
        _sessionVM = StateObject(
            wrappedValue: WorkoutSessionViewModelWrapper(
                session: session,
                localRepository: localRepository
            )
        )
    }

    var body: some View {
        Group {
            if sessionVM.isFinished {
                WorkoutSummaryView(
                    planName: (sessionVM.vm.state.value as? WorkoutSessionViewModel.State)?.session.planName ?? "Тренировка",
                    durationSeconds: Int(sessionVM.sessionDurationSeconds),
                    exerciseCount: Int(sessionVM.totalExercises),
                    totalVolumeKg: sessionVM.totalVolumeKg(),
                    exerciseSummaries: exerciseSummaries,
                    isSubmitting: sessionVM.isSubmitting,
                    isSubmitted: sessionVM.isSubmitted,
                    submitError: sessionVM.submitError,
                    onRetry: { sessionVM.retrySubmit() },
                    onDismiss: { dismiss() }
                )
            } else if sessionVM.phase == WorkoutSessionViewModel.Phase.rest {
                RestTimerView(sessionVM: sessionVM, onDismiss: { dismiss() })
            } else {
                exerciseView
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .navigationBarBackButtonHidden(true)
        .interactiveDismissDisabled(true)
    }

    private var exerciseSummaries: [WorkoutSummaryView.ExerciseSummaryData] {
        guard let state = sessionVM.vm.state.value as? WorkoutSessionViewModel.State else { return [] }
        return state.session.exercises.enumerated().map { (index, exercise) in
            let setsForExercise = state.completedSets.filter { $0.exerciseIndex == Int32(index) }
            let nonSkipped = setsForExercise.filter { !($0.repsActual == 0 && $0.weightActual == 0.0) }
            let totalSets = setsForExercise.count
            let totalReps = nonSkipped.reduce(0) { $0 + Int($1.repsActual) }
            let maxWeight = nonSkipped.map { $0.weightActual }.max() ?? 0.0
            return WorkoutSummaryView.ExerciseSummaryData(
                name: exercise.exerciseName,
                sets: totalSets,
                reps: totalReps,
                maxWeight: maxWeight
            )
        }
    }

    private var exerciseView: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: { showExitAlert = true }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Palette.deepInk)
                        .frame(width: 40, height: 40)
                        .background(Circle().fill(Color.white))
                        .shadow(color: .black.opacity(0.08), radius: 4, y: 2)
                }
                .buttonStyle(.plain)

                Spacer()

                Text("Тренировка")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Palette.deepInk)

                Spacer()

                Color.clear
                    .frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)

            VStack(spacing: 20) {
                ExerciseHeroView(
                    imageUrl: sessionVM.currentExercise?.imageUrl,
                    techniqueTip: sessionVM.currentExercise?.techniqueTip,
                    onInfoTapped: { showExerciseInfo = true }
                )

                exerciseInfoLabels

                setProgressView

                ElapsedTimerView(exerciseIndex: sessionVM.currentExerciseIndex, setIndex: sessionVM.currentSetIndex)

                Spacer()

                controlsView
            }
        }
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            Button(action: { sessionVM.completeSet(elapsedSeconds: 0) }) {
                Text("Завершить подход")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
            .background(Palette.warmOffWhite)
        }
        .alert("Завершить тренировку?", isPresented: $showExitAlert) {
            Button("Продолжить", role: .cancel) { }
            Button("Завершить", role: .destructive) {
                sessionVM.cancelWorkout()
                dismiss()
            }
        } message: {
            Text("Прогресс будет сохранён как незавершённая тренировка.")
        }
        .sheet(isPresented: $showExerciseInfo) {
            if let exerciseId = sessionVM.currentExercise?.exerciseId {
                ExerciseInfoSheet(exerciseId: exerciseId)
            }
        }
    }

    private var exerciseInfoLabels: some View {
        VStack(spacing: 8) {
            Text(sessionVM.currentExercise?.exerciseName ?? "")
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(Palette.deepInk)
                .multilineTextAlignment(.center)

            Text("Основная группа мышц: \(sessionVM.currentExercise?.muscleGroupLabel ?? "")")
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.top, 12)
        .padding(.horizontal, 16)
    }

    private var setProgressView: some View {
        VStack(spacing: 8) {
            Text("Подход \(sessionVM.displaySetNumber) из \(sessionVM.totalSets)")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Palette.deepInk)

            HStack(spacing: 8) {
                ForEach(0..<Int(sessionVM.totalSets), id: \.self) { i in
                    Circle()
                        .fill(i < Int(sessionVM.displaySetNumber) ? Palette.coral : Color.gray.opacity(0.3))
                        .frame(width: 12, height: 12)
                }
            }
        }
    }

    private var controlsView: some View {
        Group {
            if sessionVM.requiresWeight {
                HStack(spacing: 12) {
                    WorkoutControlCard(
                        label: "Вес (кг)",
                        value: sessionVM.currentWeight,
                        isDecimal: true,
                        onMinus: { sessionVM.adjustWeight(-2.5) },
                        onPlus: { sessionVM.adjustWeight(2.5) }
                    )

                    WorkoutControlCard(
                        label: "Повторы",
                        value: Double(sessionVM.currentReps),
                        isDecimal: false,
                        onMinus: { sessionVM.adjustReps(-1) },
                        onPlus: { sessionVM.adjustReps(1) }
                    )
                }
                .padding(.horizontal, 16)
            } else {
                HStack {
                    Spacer()
                    WorkoutControlCard(
                        label: "Повторы",
                        value: Double(sessionVM.currentReps),
                        isDecimal: false,
                        onMinus: { sessionVM.adjustReps(-1) },
                        onPlus: { sessionVM.adjustReps(1) }
                    )
                    .frame(width: 180)
                    Spacer()
                }
                .padding(.horizontal, 16)
            }
        }
    }
}
