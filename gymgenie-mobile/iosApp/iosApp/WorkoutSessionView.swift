import SwiftUI
import Shared

/// Top-level workout session view.
///
/// Owns the per-set elapsed timer (local UI state) and routes between the three
/// stages of the workout: active exercise, between-set rest, and final summary.
/// The summary is shown INLINE when the shared VM reports `isFinished` rather than
/// dismissing — the summary itself owns the "go home" action.
struct WorkoutSessionView: View {
    @StateObject private var sessionVM: WorkoutSessionViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    // Per-set elapsed timer (local UI state, mirrors the Android behavior).
    @State private var elapsed: Int = 0
    @State private var paused: Bool = false
    @State private var timerTask: Task<Void, Never>? = nil

    init(session: ActiveWorkoutSession) {
        // The shared session VM persists each completed set locally and submits
        // the whole session to the backend on completion. We assemble the
        // dependencies once at construction time so the wrapper stays ignorant
        // of where they came from. Token storage and the authenticated HTTP
        // client are now built inside the wrapper itself.
        let driverFactory = DatabaseDriverFactory()
        let localRepository = LocalWorkoutRepository(driverFactory: driverFactory)
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
                    isSubmitting: sessionVM.isSubmitting,
                    isSubmitted: sessionVM.isSubmitted,
                    submitError: sessionVM.submitError,
                    onRetry: { sessionVM.retrySubmit() },
                    onDismiss: { dismiss() }
                )
                .onAppear { stopElapsedTimer() }
            } else if sessionVM.phase == WorkoutSessionViewModel.Phase.rest {
                RestTimerView(sessionVM: sessionVM)
                    .onAppear { stopElapsedTimer() }
            } else {
                exerciseView
                    .onAppear { startElapsedTimer() }
                    .onChange(of: sessionVM.currentExerciseIndex) { _ in startElapsedTimer() }
                    .onChange(of: sessionVM.currentSetIndex) { _ in startElapsedTimer() }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .onDisappear { stopElapsedTimer() }
    }

    // MARK: - Elapsed timer (local, per-set)

    private func startElapsedTimer() {
        stopElapsedTimer()
        elapsed = 0
        paused = false
        timerTask = Task { @MainActor in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                if Task.isCancelled { break }
                if !paused { elapsed += 1 }
            }
        }
    }

    private func stopElapsedTimer() {
        timerTask?.cancel()
        timerTask = nil
    }

    // MARK: - Exercise execution screen

    private var exerciseView: some View {
        VStack(spacing: 0) {
            // Top bar
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Palette.deepInk)
                        .frame(width: 40, height: 40)
                        .background(Circle().fill(Color.white))
                        .shadow(color: .black.opacity(0.08), radius: 4, y: 2)
                }

                Spacer()

                VStack(spacing: 2) {
                    Text(sessionVM.currentExercise?.exerciseName ?? "")
                        .font(.system(size: 16, weight: .semibold))
                        .lineLimit(1)
                    Text(sessionVM.currentExercise?.muscleGroupLabel ?? "")
                        .font(.system(size: 12))
                        .foregroundColor(.gray)
                }

                Spacer()

                Button(action: {}) {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 18))
                        .foregroundColor(Palette.deepInk)
                        .frame(width: 40, height: 40)
                        .background(Circle().fill(Color.white))
                        .shadow(color: .black.opacity(0.08), radius: 4, y: 2)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 20) {
                    exerciseImageView
                    setProgressView
                    timerView
                    controlsView
                    Spacer(minLength: 20)
                }
                .padding(.bottom, 8)
            }
        }
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            Button(action: { sessionVM.completeSet() }) {
                HStack(spacing: 8) {
                    Text("Завершить подход")
                        .font(.system(size: 17, weight: .semibold))
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 18))
                }
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
            .background(Palette.warmOffWhite)
        }
    }

    // MARK: - Hero / image

    private var exerciseImageView: some View {
        ZStack(alignment: .topTrailing) {
            if let imageUrl = sessionVM.currentExercise?.imageUrl, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    exerciseImagePlaceholder
                }
                .frame(height: 200)
                .clipped()
                .cornerRadius(16)
            } else {
                exerciseImagePlaceholder
            }

            // Recommended weight badge
            Text("Рек: \(Int(sessionVM.currentExercise?.weightKg?.doubleValue ?? 60.0)) кг")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Capsule().fill(Color.green.opacity(0.9)))
                .padding(10)

            // Technique tip overlay (bottom)
            if let tip = sessionVM.currentExercise?.techniqueTip, !tip.isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer()
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Техника")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(.white.opacity(0.7))
                        Text(tip)
                            .font(.system(size: 11))
                            .foregroundColor(.white)
                            .lineLimit(2)
                    }
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        LinearGradient(
                            colors: [Color.clear, Color.black.opacity(0.65)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                }
                .frame(height: 200)
                .cornerRadius(16)
                .allowsHitTesting(false)
            }
        }
        .padding(.horizontal, 16)
    }

    private var exerciseImagePlaceholder: some View {
        RoundedRectangle(cornerRadius: 16)
            .fill(
                LinearGradient(
                    colors: [Color(red: 0.102, green: 0.102, blue: 0.180), Color(red: 0.176, green: 0.176, blue: 0.267)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .frame(height: 200)
            .overlay(
                Text("🏋️")
                    .font(.system(size: 64))
            )
    }

    // MARK: - Set progress

    private var setProgressView: some View {
        VStack(spacing: 8) {
            Text("Подход \(sessionVM.displaySetNumber) из \(sessionVM.totalSets)")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Palette.deepInk)

            HStack(spacing: 8) {
                ForEach(0..<Int(sessionVM.totalSets), id: \.self) { i in
                    Circle()
                        .fill(i < Int(sessionVM.displaySetNumber) ? Palette.coral : Color.gray.opacity(0.3))
                        .frame(width: 10, height: 10)
                }
            }
        }
    }

    // MARK: - Elapsed timer (per-set, counts up)

    private var timerView: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                timerBox(String(format: "%02d", elapsed / 60))
                Text(":")
                    .font(.system(size: 40, weight: .bold))
                    .foregroundColor(Palette.coral)
                timerBox(String(format: "%02d", elapsed % 60))
            }
            .padding(.horizontal, 16)

            Button(action: { paused.toggle() }) {
                Image(systemName: paused ? "play.fill" : "pause.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.coral)
                    .frame(width: 48, height: 48)
                    .background(
                        Circle()
                            .stroke(Palette.coral, lineWidth: 1.5)
                            .background(Circle().fill(Color.white))
                    )
            }
        }
    }

    private func timerBox(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 56, weight: .bold, design: .monospaced))
            .foregroundColor(Palette.deepInk)
            .frame(maxWidth: .infinity)
            .frame(height: 88)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Palette.coralLight)
            )
    }

    // MARK: - Weight + reps controls

    private var controlsView: some View {
        HStack(spacing: 12) {
            controlCard(label: "Вес (кг)", value: sessionVM.currentWeight, isDecimal: true) {
                sessionVM.adjustWeight(-1.0)
            } onPlus: {
                sessionVM.adjustWeight(1.0)
            }

            controlCard(label: "Повторы", value: Double(sessionVM.currentReps), isDecimal: false) {
                sessionVM.adjustReps(-1)
            } onPlus: {
                sessionVM.adjustReps(1)
            }
        }
        .padding(.horizontal, 16)
    }

    private func controlCard(
        label: String,
        value: Double,
        isDecimal: Bool,
        onMinus: @escaping () -> Void,
        onPlus: @escaping () -> Void
    ) -> some View {
        VStack(spacing: 12) {
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.gray)

            HStack(spacing: 16) {
                Button(action: onMinus) {
                    Image(systemName: "minus")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Palette.coral)
                        .frame(width: 36, height: 36)
                        .background(
                            Circle()
                                .stroke(Palette.coral, lineWidth: 1.5)
                                .background(Circle().fill(Color.white))
                        )
                }

                Text(isDecimal ? String(format: "%.0f", value) : String(Int(value)))
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(Palette.deepInk)
                    .frame(minWidth: 40)

                Button(action: onPlus) {
                    Image(systemName: "plus")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(Palette.coral))
                        .shadow(color: Palette.coral.opacity(0.4), radius: 4, y: 2)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(RoundedRectangle(cornerRadius: 16).fill(.white))
        .shadow(color: .black.opacity(0.05), radius: 6, y: 2)
    }
}
