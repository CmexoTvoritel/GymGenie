import SwiftUI
import Shared

/// Between-sets rest screen.
///
/// Shows a circular countdown, ±10s adjustments, the next-up exercise preview, and two
/// terminal actions: skip current set (records it as skipped) and skip rest (advances
/// without recording).
struct RestTimerView: View {
    @ObservedObject var sessionVM: WorkoutSessionViewModelWrapper

    private var totalRestSeconds: Int {
        let configured = Int(sessionVM.vm.state.value?.session.restSeconds ?? 60)
        // Guard against negative ratios when the user adds time mid-rest.
        return max(configured, Int(sessionVM.restSecondsRemaining))
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Spacer()
                Text(sessionVM.currentExercise?.exerciseName ?? "Отдых")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 24)

            Spacer()

            circularTimer
                .padding(.horizontal, 40)

            Spacer()

            // ± 10s adjustments
            HStack(spacing: 12) {
                adjustButton(label: "- 10 сек") { sessionVM.adjustRest(-10) }
                adjustButton(label: "+ 10 сек") { sessionVM.adjustRest(10) }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)

            // Next exercise / set preview
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
    }

    // MARK: - Components

    private var circularTimer: some View {
        let remaining = Int(sessionVM.restSecondsRemaining)
        let total = totalRestSeconds
        let progress = total > 0 ? Double(remaining) / Double(total) : 0.0
        let minutes = remaining / 60
        let seconds = remaining % 60

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
                .animation(.linear(duration: 1), value: sessionVM.restSecondsRemaining)

            VStack(spacing: 4) {
                Text(String(format: "%02d:%02d", minutes, seconds))
                    .font(.system(size: 52, weight: .bold, design: .monospaced))
                    .foregroundColor(Palette.deepInk)
                Text("отдых")
                    .font(.system(size: 16))
                    .foregroundColor(.gray)
            }
        }
        .frame(width: 240, height: 240)
    }

    private func adjustButton(label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(Palette.deepInk)
                .frame(maxWidth: .infinity)
                .frame(height: 46)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.3), lineWidth: 1.5)
                        .background(RoundedRectangle(cornerRadius: 12).fill(.white))
                )
        }
    }

    private var nextExerciseCard: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Palette.coralLight)
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: "dumbbell")
                        .font(.system(size: 16))
                        .foregroundColor(Palette.coral)
                )

            VStack(alignment: .leading, spacing: 2) {
                Text("Далее")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
                Text(sessionVM.nextExercise?.exerciseName ?? "Финиш")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                Text(nextSetLabel)
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.system(size: 14))
                .foregroundColor(.gray)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14).fill(.white))
        .shadow(color: .black.opacity(0.05), radius: 6, y: 2)
    }

    private var skipSetButton: some View {
        Button(action: { sessionVM.skipSet() }) {
            Text("Пропустить подход")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(Palette.deepInk)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.3), lineWidth: 1.5)
                        .background(RoundedRectangle(cornerRadius: 12).fill(.white))
                )
        }
    }

    private var skipRestButton: some View {
        Button(action: { sessionVM.skipRest() }) {
            HStack(spacing: 8) {
                Text("Пропустить отдых")
                    .font(.system(size: 17, weight: .semibold))
                Image(systemName: "forward.end.fill")
                    .font(.system(size: 16))
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
        }
    }

    /// Mirrors Android's `nextSetLabel` logic. The current set index still points at the set the
    /// user just completed, so the visible "next" set within the same exercise is `currentSetIndex + 2`.
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
