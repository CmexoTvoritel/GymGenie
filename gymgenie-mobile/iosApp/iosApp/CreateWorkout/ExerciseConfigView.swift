import SwiftUI
import Shared

/// Step 3 — configure sets / reps for the chosen exercise before it is added
/// to the pending workout.
///
/// The view does not mutate the shared ViewModel directly. It assembles a
/// `PendingExercise` and hands it to `onConfirm`, letting the parent decide
/// whether to push, replace, or merge.
struct ExerciseConfigView: View {
    let exercise: Shared.ExerciseShortResponse
    let onBack: () -> Void
    let onConfirm: (Shared.PendingExercise) -> Void

    @State private var sets: Int = Int(Shared.CreateWorkoutLimits.shared.DEFAULT_SETS)
    @State private var reps: Int = Int(Shared.CreateWorkoutLimits.shared.DEFAULT_REPS)

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    private var minSets: Int { Int(Shared.CreateWorkoutLimits.shared.MIN_SETS) }
    private var maxSets: Int { Int(Shared.CreateWorkoutLimits.shared.MAX_SETS) }
    private var minReps: Int { Int(Shared.CreateWorkoutLimits.shared.MIN_REPS) }
    private var maxReps: Int { Int(Shared.CreateWorkoutLimits.shared.MAX_REPS) }

    var body: some View {
        ZStack {
            warmOffWhite.edgesIgnoringSafeArea(.all)

            VStack(spacing: 0) {
                header

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 16) {
                        exerciseSummary
                        paramsCard
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 8)
                }

                bottomBar
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(.white))
                    .shadow(color: Color.black.opacity(0.06), radius: 2, y: 1)
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 2) {
                Text("Параметры упражнения")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(deepInk)
                Text("Сколько подходов и повторений")
                    .font(.system(size: 12))
                    .foregroundColor(mutedText)
            }

            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 16)
    }

    private var exerciseSummary: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 14).fill(softCard)
                    .frame(width: 64, height: 64)
                Text(muscleGroupEmoji(exercise.muscleGroup))
                    .font(.system(size: 30))
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(exercise.nameRu)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(deepInk)
                    .lineLimit(2)
                if !exercise.nameEn.isEmpty {
                    Text(exercise.nameEn)
                        .font(.system(size: 12))
                        .foregroundColor(mutedText)
                        .lineLimit(1)
                }
            }

            Spacer()
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 16).fill(.white))
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
    }

    private var paramsCard: some View {
        VStack(spacing: 0) {
            stepperRow(
                title: "Подходы",
                value: sets,
                minValue: minSets,
                maxValue: maxSets,
                onDecrement: { if sets > minSets { sets -= 1 } },
                onIncrement: { if sets < maxSets { sets += 1 } }
            )

            Divider()
                .padding(.horizontal, 4)

            stepperRow(
                title: "Повторений",
                value: reps,
                minValue: minReps,
                maxValue: maxReps,
                onDecrement: { if reps > minReps { reps -= 1 } },
                onIncrement: { if reps < maxReps { reps += 1 } }
            )
        }
        .padding(.vertical, 4)
        .background(RoundedRectangle(cornerRadius: 16).fill(softCard))
    }

    private func stepperRow(
        title: String,
        value: Int,
        minValue: Int,
        maxValue: Int,
        onDecrement: @escaping () -> Void,
        onIncrement: @escaping () -> Void
    ) -> some View {
        HStack {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(deepInk)

            Spacer()

            HStack(spacing: 12) {
                stepperButton(
                    symbol: "minus",
                    disabled: value <= minValue,
                    action: onDecrement
                )

                Text("\(value)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(deepInk)
                    .frame(minWidth: 32)

                stepperButton(
                    symbol: "plus",
                    disabled: value >= maxValue,
                    action: onIncrement
                )
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    private func stepperButton(symbol: String, disabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 44, height: 44)
                .background(Circle().fill(disabled ? Color.gray.opacity(0.4) : orange))
        }
        .buttonStyle(.plain)
        .disabled(disabled)
    }

    // MARK: - Bottom CTA

    private var bottomBar: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [warmOffWhite.opacity(0), warmOffWhite],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 16)

            Button(action: confirm) {
                Text("Добавить упражнение")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(RoundedRectangle(cornerRadius: 14).fill(orange))
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
            .background(warmOffWhite)
        }
    }

    private func confirm() {
        let pending = Shared.PendingExercise(
            exerciseId: exercise.id,
            exerciseNameRu: exercise.nameRu,
            exerciseNameEn: exercise.nameEn,
            muscleGroupKey: exercise.muscleGroup,
            sets: Int32(sets),
            reps: Int32(reps)
        )
        onConfirm(pending)
    }
}
