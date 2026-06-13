import SwiftUI
import Shared

struct RestTimerView: View {
    @ObservedObject var sessionVM: WorkoutSessionViewModelWrapper
    var onDismiss: () -> Void

    @State private var showExitAlert: Bool = false
    @State private var showNextExerciseInfo: Bool = false
    @State private var restEndDate: Date = Date()
    @State private var restCompletionTask: Task<Void, Never>? = nil
    @State private var remainingSeconds: Int = 0
    @State private var circleMax: Int = 0
    @State private var countdownTask: Task<Void, Never>? = nil

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Тренировка",
                showBackNavigation: true,
                useCloseIcon: true,
                onBackTap: { showExitAlert = true }
            )

            Spacer()

            circularTimer
                .padding(.horizontal, 40)

            Spacer()

            HStack(spacing: 12) {
                adjustButton(label: "- 10 сек") {
                    sessionVM.adjustRest(-10)
                    restEndDate = max(Date().addingTimeInterval(1), restEndDate.addingTimeInterval(-10))
                    remainingSeconds = max(0, Int(ceil(restEndDate.timeIntervalSince(Date()))))
                    scheduleRestCompletion()
                }
                adjustButton(label: "+ 10 сек") {
                    sessionVM.adjustRest(10)
                    restEndDate = restEndDate.addingTimeInterval(10)
                    remainingSeconds = max(0, Int(ceil(restEndDate.timeIntervalSince(Date()))))
                    circleMax = max(circleMax, remainingSeconds)
                    scheduleRestCompletion()
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)

            nextExerciseCard
                .padding(.horizontal, 16)
                .padding(.bottom, 20)
        }
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 8) {
                skipSetButton
                skipRestButton
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
            .background(Palette.warmOffWhite)
        }
        .alert("Завершить тренировку?", isPresented: $showExitAlert) {
            Button("Продолжить", role: .cancel) { }
            Button("Завершить", role: .destructive) {
                sessionVM.cancelWorkout()
                onDismiss()
            }
        } message: {
            Text("Прогресс будет сохранён как незавершённая тренировка.")
        }
        .sheet(isPresented: $showNextExerciseInfo) {
            if let exerciseId = nextExerciseInfoId {
                ExerciseInfoSheet(exerciseId: exerciseId)
            }
        }
        .onAppear {
            restEndDate = Date().addingTimeInterval(Double(sessionVM.restDurationSeconds))
            scheduleRestCompletion()
            remainingSeconds = Int(ceil(Double(sessionVM.restDurationSeconds)))
            circleMax = remainingSeconds
            startCountdown()
        }
        .onDisappear {
            restCompletionTask?.cancel()
            countdownTask?.cancel()
        }
    }

    private func scheduleRestCompletion() {
        restCompletionTask?.cancel()
        let endDate = restEndDate
        restCompletionTask = Task { @MainActor in
            let remaining = endDate.timeIntervalSince(Date())
            if remaining > 0 {
                try? await Task.sleep(nanoseconds: UInt64(remaining * 1_000_000_000))
            }
            if !Task.isCancelled {
                sessionVM.restComplete()
            }
        }
    }

    private func startCountdown() {
        countdownTask?.cancel()
        countdownTask = Task { @MainActor in
            while !Task.isCancelled {
                let remaining = restEndDate.timeIntervalSince(Date())
                remainingSeconds = max(0, Int(ceil(remaining)))
                if remaining <= 0 { break }
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }

    private var circularTimer: some View {
        let total = max(1.0, Double(circleMax))
        let progress = Double(remainingSeconds) / total
        let minutes = remainingSeconds / 60
        let seconds = remainingSeconds % 60

        return ZStack {
            Circle()
                .stroke(Color.gray.opacity(0.15), lineWidth: 12)

            Circle()
                .trim(from: 0, to: CGFloat(progress))
                .stroke(
                    Palette.coral,
                    style: StrokeStyle(lineWidth: 12, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.linear(duration: 1), value: remainingSeconds)

            VStack(spacing: 4) {
                Text(String(format: "%02d:%02d", minutes, seconds))
                    .font(.system(size: 52, weight: .bold, design: .monospaced))
                    .foregroundColor(Palette.deepInk)
                Text("отдых")
                    .font(.system(size: 18))
                    .foregroundColor(.gray)
            }
        }
        .frame(width: 240, height: 240)
    }

    private func adjustButton(label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 19, weight: .medium))
                .foregroundColor(Palette.deepInk)
                .frame(maxWidth: .infinity)
                .frame(height: 46)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Palette.coral.opacity(0.4), lineWidth: 1.5)
                        .background(RoundedRectangle(cornerRadius: 12).fill(Palette.coral.opacity(0.05)))
                )
        }
        .buttonStyle(.plain)
    }

    private var nextExerciseCard: some View {
        HStack(spacing: 12) {
            Image(muscleGroupExerciseImageName(nextMuscleGroup))
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 36, height: 36)
                .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 2) {
                Text("Далее")
                    .font(.system(size: 15))
                    .foregroundColor(.gray)
                Text(nextExerciseName)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                Text(nextSetLabel)
                    .font(.system(size: 15))
                    .foregroundColor(.gray)
            }

            Spacer()

            Button(action: { showNextExerciseInfo = true }) {
                Image(systemName: "info.circle")
                    .font(.system(size: 22))
                    .foregroundColor(Palette.coral)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Palette.coral.opacity(0.12)))
            }
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14).fill(.white))
        .shadow(color: .black.opacity(0.05), radius: 6, y: 2)
    }

    private var skipSetButton: some View {
        Button(action: { sessionVM.skipSet() }) {
            Text("Пропустить подход")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(Palette.deepInk)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.3), lineWidth: 1.5)
                )
        }
        .buttonStyle(.plain)
    }

    private var skipRestButton: some View {
        Button(action: { sessionVM.skipRest() }) {
            Text("Пропустить отдых")
                .font(.system(size: 19, weight: .semibold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
        }
        .buttonStyle(.plain)
    }

    private var nextExerciseName: String {
        let currentSets = Int(sessionVM.totalSets)
        let nextWithinExercise = Int(sessionVM.currentSetIndex) + 2
        if nextWithinExercise <= currentSets {
            return sessionVM.currentExercise?.exerciseName ?? "Финиш"
        }
        return sessionVM.nextExercise?.exerciseName ?? "Финиш"
    }

    private var nextExerciseInfoId: String? {
        let currentSets = Int(sessionVM.totalSets)
        let nextWithinExercise = Int(sessionVM.currentSetIndex) + 2
        if nextWithinExercise <= currentSets {
            return sessionVM.currentExercise?.exerciseId
        }
        return sessionVM.nextExercise?.exerciseId
    }

    private var nextMuscleGroup: String {
        let currentSets = Int(sessionVM.totalSets)
        let nextWithinExercise = Int(sessionVM.currentSetIndex) + 2
        if nextWithinExercise <= currentSets {
            return sessionVM.currentExercise?.muscleGroupLabel ?? ""
        }
        return sessionVM.nextExercise?.muscleGroupLabel ?? ""
    }

    private var nextSetLabel: String {
        let currentSets = Int(sessionVM.totalSets)
        let nextWithinExercise = Int(sessionVM.currentSetIndex) + 2
        if nextWithinExercise <= currentSets {
            return "Подход \(nextWithinExercise) из \(currentSets)"
        }
        if let next = sessionVM.nextExercise {
            return "Подход 1 из \(Int(next.sets))"
        }
        return "Финиш"
    }
}
