import SwiftUI
import Shared

struct ExerciseConfigView: View {
    let exercise: Shared.ExerciseShortResponse
    let onBack: () -> Void
    let onConfirm: (Shared.PendingExercise) -> Void
    let prefillFrom: Shared.PendingExercise?
    let showStepHeader: Bool

    @State private var sets: Int
    @State private var reps: Int

    @State private var weightMode: WeightMode
    @State private var uniformWeightKg: Double
    @State private var perSetWeightsKg: [Double]

    @State private var detailExerciseId: String? = nil

    init(
        exercise: Shared.ExerciseShortResponse,
        onBack: @escaping () -> Void,
        onConfirm: @escaping (Shared.PendingExercise) -> Void,
        prefillFrom: Shared.PendingExercise? = nil,
        showStepHeader: Bool = true
    ) {
        self.exercise = exercise
        self.onBack = onBack
        self.onConfirm = onConfirm
        self.prefillFrom = prefillFrom
        self.showStepHeader = showStepHeader

        let defaultSets = Int(Shared.CreateWorkoutLimits.shared.DEFAULT_SETS)
        let defaultReps = Int(Shared.CreateWorkoutLimits.shared.DEFAULT_REPS)
        let defaultWeight = Shared.CreateWorkoutLimits.shared.DEFAULT_WEIGHT_KG

        let initialSets = prefillFrom.map { Int($0.sets) } ?? defaultSets
        let initialReps = prefillFrom.map { Int($0.reps) } ?? defaultReps

        let prefilledWeights: [Double] = (prefillFrom?.setWeightsKg ?? []).compactMap {
            ($0 as? KotlinDouble)?.doubleValue
        }
        let weightFallback = prefilledWeights.first ?? defaultWeight

        let initialMode: WeightMode = (Set(prefilledWeights).count > 1) ? .perSet : .uniform
        let initialUniform = weightFallback

        var initialPerSet: [Double]
        if prefillFrom != nil {
            initialPerSet = prefilledWeights
            if initialPerSet.count < initialSets {
                initialPerSet.append(
                    contentsOf: Array(
                        repeating: initialPerSet.last ?? weightFallback,
                        count: initialSets - initialPerSet.count
                    )
                )
            } else if initialPerSet.count > initialSets {
                initialPerSet = Array(initialPerSet.prefix(initialSets))
            }
        } else {
            initialPerSet = Array(repeating: defaultWeight, count: initialSets)
        }

        _sets = State(initialValue: initialSets)
        _reps = State(initialValue: initialReps)
        _weightMode = State(initialValue: initialMode)
        _uniformWeightKg = State(initialValue: initialUniform)
        _perSetWeightsKg = State(initialValue: initialPerSet)
    }

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    private var minSets: Int { Int(Shared.CreateWorkoutLimits.shared.MIN_SETS) }
    private var maxSets: Int { Int(Shared.CreateWorkoutLimits.shared.MAX_SETS) }
    private var minReps: Int { Int(Shared.CreateWorkoutLimits.shared.MIN_REPS) }
    private var maxReps: Int { Int(Shared.CreateWorkoutLimits.shared.MAX_REPS) }

    private var minWeight: Double { Shared.CreateWorkoutLimits.shared.MIN_WEIGHT_KG }
    private var maxWeight: Double { Shared.CreateWorkoutLimits.shared.MAX_WEIGHT_KG }
    private var weightStep: Double { Shared.CreateWorkoutLimits.shared.WEIGHT_STEP_KG }

    private enum WeightMode { case uniform, perSet }

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: exercise.nameRu,
                showBackNavigation: true,
                onBackTap: onBack
            )
            if showStepHeader {
                WorkoutFlowStepHeader(currentStep: 3, totalSteps: 3)
            }

            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    exerciseSummary
                    paramsCard
                    if exercise.requiresWeight {
                        weightCard
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 24)
            }

            bottomBar
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(warmOffWhite.ignoresSafeArea())
        .onChange(of: sets) { newCount in

            if perSetWeightsKg.count != newCount {
                if newCount > perSetWeightsKg.count {
                    let seed = perSetWeightsKg.last ?? uniformWeightKg
                    perSetWeightsKg.append(
                        contentsOf: Array(repeating: seed, count: newCount - perSetWeightsKg.count)
                    )
                } else {
                    perSetWeightsKg = Array(perSetWeightsKg.prefix(newCount))
                }
            }
        }
        .sheet(
            isPresented: Binding(
                get: { detailExerciseId != nil },
                set: { if !$0 { detailExerciseId = nil } }
            )
        ) {
            if let id = detailExerciseId {
                ExerciseDetailView(
                    exerciseId: id,
                    onBack: { detailExerciseId = nil },
                    onAddToWorkout: { _ in detailExerciseId = nil },
                    showAddButton: false
                )
            }
        }
    }

    private var exerciseSummary: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous).fill(softCard)
                    .frame(width: 64, height: 64)
                Image(muscleGroupExerciseImageName(exercise.muscleGroup))
                    .resizable()
                    .aspectRatio(1, contentMode: .fit)
                    .frame(width: 64, height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(exercise.nameRu)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(deepInk)
                    .lineLimit(2)
                if !exercise.muscleGroup.isEmpty {
                    Text(muscleGroupNameRu(exercise.muscleGroup))
                        .font(.system(size: 14))
                        .foregroundColor(mutedText)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 8)

            Button {
                detailExerciseId = exercise.id
            } label: {
                Image(systemName: "info.circle")
                    .font(.system(size: 22, weight: .medium))
                    .foregroundColor(orange)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(orange.opacity(0.12)))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Подробнее об упражнении")
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
                title: "Повторений в подходе",
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

    private var weightCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Вес")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(deepInk)

            weightModeSelector

            switch weightMode {
            case .uniform:
                weightStepperRow(
                    title: "Вес (кг)",
                    value: uniformWeightKg,
                    onChange: { uniformWeightKg = clampedWeight($0) }
                )
            case .perSet:
                VStack(spacing: 8) {
                    ForEach(0..<perSetWeightsKg.count, id: \.self) { index in
                        weightStepperRow(
                            title: "Подход \(index + 1)",
                            value: perSetWeightsKg[index],
                            onChange: { newValue in
                                guard index < perSetWeightsKg.count else { return }
                                perSetWeightsKg[index] = clampedWeight(newValue)
                            }
                        )
                    }
                }
            }
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 16).fill(.white))
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
    }

    private var weightModeSelector: some View {
        HStack(spacing: 4) {
            weightModeOption(label: "Одинаковый вес", isSelected: weightMode == .uniform) {
                weightMode = .uniform
            }
            weightModeOption(label: "Разный вес", isSelected: weightMode == .perSet) {
                weightMode = .perSet
            }
        }
        .padding(4)
        .background(RoundedRectangle(cornerRadius: 14).fill(softCard))
    }

    private func weightModeOption(
        label: String,
        isSelected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(isSelected ? .white : mutedText)
                .frame(maxWidth: .infinity)
                .frame(height: 38)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(isSelected ? orange : Color.clear)
                )
        }
        .buttonStyle(.plain)
    }

    private func weightStepperRow(
        title: String,
        value: Double,
        onChange: @escaping (Double) -> Void
    ) -> some View {
        HStack {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(deepInk)

            Spacer()

            HStack(spacing: 12) {
                stepperButton(
                    symbol: "minus",
                    disabled: value <= minWeight,
                    action: { onChange(value - weightStep) }
                )

                Text(formatWeightKg(value))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(deepInk)
                    .frame(minWidth: 72)

                stepperButton(
                    symbol: "plus",
                    disabled: value >= maxWeight,
                    action: { onChange(value + weightStep) }
                )
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(RoundedRectangle(cornerRadius: 12).fill(softCard))
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
                .font(.system(size: 17, weight: .semibold))
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

    private var bottomBar: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [warmOffWhite.opacity(0), warmOffWhite],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 16)

            Button(action: confirm) {
                Text(prefillFrom != nil ? "Сохранить изменения" : "Добавить упражнение")
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

        let weightsForPayload: [Double]? = exercise.requiresWeight
            ? buildWeightsForPayload()
            : nil

        let bridgedWeights: [KotlinDouble]? = weightsForPayload?.map { KotlinDouble(double: $0) }

        let pending = Shared.PendingExercise(
            exerciseId: exercise.id,
            exerciseNameRu: exercise.nameRu,
            exerciseNameEn: exercise.nameEn,
            muscleGroupKey: exercise.muscleGroup,
            sets: Int32(sets),
            reps: Int32(reps),
            requiresWeight: exercise.requiresWeight,
            setWeightsKg: bridgedWeights
        )
        onConfirm(pending)
    }

    private func buildWeightsForPayload() -> [Double] {
        switch weightMode {
        case .uniform:
            return Array(repeating: clampedWeight(uniformWeightKg), count: sets)
        case .perSet:
            var clamped = perSetWeightsKg.map(clampedWeight)
            if clamped.count < sets {
                let seed = clamped.last ?? uniformWeightKg
                clamped.append(contentsOf: Array(repeating: seed, count: sets - clamped.count))
            } else if clamped.count > sets {
                clamped = Array(clamped.prefix(sets))
            }
            return clamped
        }
    }

    private func clampedWeight(_ value: Double) -> Double {
        max(minWeight, min(maxWeight, value))
    }

    private func formatWeightKg(_ value: Double) -> String {
        let rounded = (value * 10).rounded() / 10
        if rounded.truncatingRemainder(dividingBy: 1) == 0 {
            return "\(Int(rounded)) кг"
        }
        return String(format: "%.1f кг", rounded)
    }

}
