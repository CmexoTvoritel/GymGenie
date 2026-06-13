import SwiftUI
import Shared

struct WorkoutBuilderView: View {
    @ObservedObject var vm: CreateWorkoutViewModelWrapper
    let onBack: () -> Void
    let onAddExercise: () -> Void
    let onEditExercise: (Int, Shared.PendingExercise) -> Void

    @State private var localName: String = ""
    @State private var localDescription: String = ""
    @State private var dragFromIndex: Int? = nil
    @State private var dragOffset: CGFloat = 0
    @State private var rowHeight: CGFloat = 68

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    private var minRest: Int32 { Int32(Shared.CreateWorkoutLimits.shared.MIN_REST_SECONDS) }
    private var maxRest: Int32 { Int32(Shared.CreateWorkoutLimits.shared.MAX_REST_SECONDS) }

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                header

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 16) {
                        nameField
                        descriptionField
                        scheduleTypeCard
                        if vm.scheduleType == .recurring {
                            scheduleDaysCard
                        }
                        restCard
                        exercisesSection
                        Color.clear.frame(height: 160)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 8)
                }
                .contentShape(Rectangle())
                .simultaneousGesture(TapGesture().onEnded { hideKeyboard() })
            }

            bottomBar
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(warmOffWhite.ignoresSafeArea())
        .onAppear {
            if localName != vm.workoutName { localName = vm.workoutName }
            if localDescription != vm.workoutDescription { localDescription = vm.workoutDescription }
        }
        .alert(
            "Ошибка",
            isPresented: Binding(
                get: { vm.errorMessage != nil && !vm.isSaving },
                set: { if !$0 { vm.clearError() } }
            ),
            actions: {
                Button("OK", role: .cancel) { vm.clearError() }
            },
            message: {
                Text(vm.errorMessage ?? "")
            }
        )
    }

    private var header: some View {
        GymGenieToolbar(
            title: "Создание тренировки",
            showBackNavigation: true,
            useCloseIcon: true,
            onBackTap: onBack
        )
    }

    private var nameField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Название")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(mutedText)

            TextField("Например: Грудь и трицепс", text: $localName)
                .font(.system(size: 16))
                .foregroundColor(deepInk)
                .padding(14)
                .background(RoundedRectangle(cornerRadius: 14).fill(.white))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(deepInk.opacity(0.08), lineWidth: 1)
                )
                .onChange(of: localName) { newValue in
                    vm.setWorkoutName(newValue)
                }
        }
    }

    private var descriptionField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Описание")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(mutedText)

            TextField("Короткое описание (необязательно)", text: $localDescription, axis: .vertical)
                .font(.system(size: 16))
                .foregroundColor(deepInk)
                .lineLimit(2...4)
                .padding(14)
                .background(RoundedRectangle(cornerRadius: 14).fill(.white))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(deepInk.opacity(0.08), lineWidth: 1)
                )
                .onChange(of: localDescription) { newValue in
                    if newValue.count <= 500 {
                        vm.setDescription(newValue)
                    } else {
                        localDescription = String(newValue.prefix(500))
                    }
                }
        }
    }

    private var restCard: some View {
        HStack(spacing: 12) {
            Image(systemName: "clock")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(orange)
                .frame(width: 36, height: 36)
                .background(Circle().fill(orange.opacity(0.12)))

            Text("Отдых между подходами")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(deepInk)

            Spacer()

            HStack(spacing: 8) {
                smallStepperButton(
                    symbol: "minus",
                    disabled: vm.restSeconds <= minRest,
                    action: { vm.decrementRestSeconds() }
                )
                Text(restDisplay(seconds: vm.restSeconds))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(deepInk)
                    .frame(width: 64, alignment: .center)
                smallStepperButton(
                    symbol: "plus",
                    disabled: vm.restSeconds >= maxRest,
                    action: { vm.incrementRestSeconds() }
                )
            }
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 16).fill(softCard))
    }

    private var scheduleTypeCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Тип тренировки")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(mutedText)

            HStack(spacing: 6) {
                scheduleTypeOption(label: "Разовая", type: .oneTime)
                scheduleTypeOption(label: "Постоянная", type: .recurring)
            }
            .padding(4)
            .background(RoundedRectangle(cornerRadius: 14).fill(softCard))
        }
    }

    private func scheduleTypeOption(
        label: String,
        type: Shared.WorkoutScheduleType
    ) -> some View {
        let isSelected = vm.scheduleType == type
        return Button(action: { vm.setScheduleType(type) }) {
            Text(label)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(isSelected ? .white : deepInk)
                .frame(maxWidth: .infinity)
                .frame(height: 38)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(isSelected ? orange : Color.clear)
                )
        }
        .buttonStyle(.plain)
    }

    private static let weekdayKeys: [String] = [
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
        "FRIDAY", "SATURDAY", "SUNDAY",
    ]
    private static let weekdayLabels: [String] = [
        "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс",
    ]

    private var scheduleDaysCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Дни недели")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(mutedText)

            HStack(spacing: 6) {
                ForEach(Array(zip(Self.weekdayKeys, Self.weekdayLabels)), id: \.0) { key, label in
                    dayBadge(key: key, label: label)
                }
            }
        }
    }

    private func dayBadge(key: String, label: String) -> some View {
        let isSelected = vm.scheduleDays.contains(key)
        return Button(action: { vm.toggleScheduleDay(key) }) {
            Text(label)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(isSelected ? .white : deepInk)
                .frame(maxWidth: .infinity)
                .frame(height: 40)
                .background(
                    Circle()
                        .fill(isSelected ? orange : softCard)
                )
        }
        .buttonStyle(.plain)
    }

    private func smallStepperButton(symbol: String, disabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 36, height: 36)
                .background(Circle().fill(disabled ? Color.gray.opacity(0.4) : orange))
        }
        .buttonStyle(.plain)
        .disabled(disabled)
    }

    private var exercisesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Упражнения")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(mutedText)
                Spacer()
                Text("\(vm.exercises.count)")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 3)
                    .background(Capsule().fill(orange))
            }

            if vm.exercises.isEmpty {
                emptyExercisesState
            } else {
                VStack(spacing: 10) {
                    ForEach(Array(vm.exercises.enumerated()), id: \.offset) { index, exercise in
                        exerciseRow(exercise, index: index)
                            .zIndex(dragFromIndex == index ? 1 : 0)
                            .offset(y: dragYOffset(for: index))
                            .animation(.easeInOut(duration: 0.2), value: dragFromIndex)
                            .onLongPressGesture(minimumDuration: 0.3) {
                                let impact = UIImpactFeedbackGenerator(style: .medium)
                                impact.impactOccurred()
                                dragFromIndex = index
                            }
                            .simultaneousGesture(
                                dragFromIndex != nil
                                ? DragGesture()
                                    .onChanged { value in
                                        dragOffset = value.translation.height
                                    }
                                    .onEnded { _ in
                                        if let from = dragFromIndex {
                                            let to = clampedTargetIndex(from: from, offset: dragOffset)
                                            if from != to {
                                                vm.moveExercise(from: from, to: to)
                                            }
                                        }
                                        dragFromIndex = nil
                                        dragOffset = 0
                                    }
                                : nil
                            )
                            .background(
                                GeometryReader { geo in
                                    Color.clear.onAppear { rowHeight = geo.size.height + 10 }
                                }
                            )
                    }
                }
            }
        }
    }

    private var emptyExercisesState: some View {
        VStack(spacing: 6) {
            Image(systemName: "puzzlepiece")
                .font(.system(size: 32))
                .foregroundColor(mutedText)
            Text("Пока нет упражнений")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(deepInk)
            Text("Нажмите «Добавить упражнение» ниже")
                .font(.system(size: 12))
                .foregroundColor(mutedText)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .background(RoundedRectangle(cornerRadius: 14).fill(softCard))
    }

    private func exerciseRow(_ exercise: Shared.PendingExercise, index: Int) -> some View {
        HStack(spacing: 0) {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous).fill(softCard)
                    .frame(width: 40, height: 40)
                Image(muscleGroupExerciseImageName(exercise.muscleGroupKey))
                    .resizable()
                    .aspectRatio(1, contentMode: .fit)
                    .frame(width: 40, height: 40)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(exercise.exerciseNameRu)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(deepInk)
                    .lineLimit(2)
                Text(exerciseRowSubtitle(exercise))
                    .font(.system(size: 14))
                    .foregroundColor(mutedText)
            }
            .padding(.leading, 12)
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: { onEditExercise(index, exercise) }) {
                Image(systemName: "pencil")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(deepInk)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(softCard))
            }
            .buttonStyle(.plain)

            Spacer().frame(width: 8)

            Button(action: { vm.removeExercise(at: index) }) {
                Image(systemName: "trash")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(Color(red: 0.898, green: 0.224, blue: 0.208).opacity(0.12)))
            }
            .buttonStyle(.plain)
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 12).fill(.white))
        .shadow(color: Color.black.opacity(0.04), radius: 3, y: 1)
    }

    private func exerciseRowSubtitle(_ exercise: Shared.PendingExercise) -> String {
        let base = "\(exercise.sets) подх • \(exercise.reps) пов"
        guard exercise.requiresWeight,
              let raw = exercise.setWeightsKg
        else { return base }

        let weights: [Double] = raw.compactMap { ($0 as? KotlinDouble)?.doubleValue }
        guard !weights.isEmpty else { return base }

        let unique = Array(Set(weights))
        if unique.count == 1 {
            return "\(base) • \(formatWeightShort(unique[0])) кг"
        }
        let minW = weights.min() ?? 0
        let maxW = weights.max() ?? 0
        return "\(base) • \(formatWeightShort(minW))-\(formatWeightShort(maxW)) кг"
    }

    private func formatWeightShort(_ kg: Double) -> String {
        if kg.truncatingRemainder(dividingBy: 1) == 0 {
            return String(Int(kg))
        }
        return String(format: "%.1f", kg)
    }

    private var bottomBar: some View {
        VStack(spacing: 10) {
            LinearGradient(
                colors: [warmOffWhite.opacity(0), warmOffWhite],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 12)

            VStack(spacing: 10) {
                Button(action: onAddExercise) {
                    HStack(spacing: 6) {
                        Image(systemName: "plus")
                            .font(.system(size: 14, weight: .bold))
                        Text("Добавить упражнение")
                            .font(.system(size: 17, weight: .semibold))
                    }
                    .foregroundColor(orange)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(orange, lineWidth: 1.5)
                    )
                }
                .buttonStyle(.plain)

                Button(action: { vm.saveWorkout() }) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(saveButtonDisabled ? Color.gray.opacity(0.4) : orange)

                        if vm.isSaving {
                            ProgressView().tint(.white)
                        } else {
                            Text("Сохранить")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }
                    .frame(height: 54)
                }
                .buttonStyle(.plain)
                .disabled(saveButtonDisabled)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
            .background(warmOffWhite)
        }
    }

    private var saveButtonDisabled: Bool {
        vm.isSaving ||
        vm.exercises.isEmpty ||
        localName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func restDisplay(seconds: Int32) -> String {
        let s = Int(seconds)
        if s < 60 { return "\(s)с" }
        let minutes = s / 60
        let remainder = s % 60
        if remainder == 0 { return "\(minutes)м" }
        return "\(minutes)м \(remainder)с"
    }

    private func dragYOffset(for index: Int) -> CGFloat {
        guard let from = dragFromIndex else { return 0 }
        if index == from { return dragOffset }
        let targetIndex = clampedTargetIndex(from: from, offset: dragOffset)
        if from < targetIndex && index > from && index <= targetIndex {
            return -rowHeight
        } else if from > targetIndex && index < from && index >= targetIndex {
            return rowHeight
        }
        return 0
    }

    private func clampedTargetIndex(from: Int, offset: CGFloat) -> Int {
        let steps = Int(round(offset / rowHeight))
        let target = from + steps
        return max(0, min(target, vm.exercises.count - 1))
    }

    private func hideKeyboard() {
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil, from: nil, for: nil
        )
    }
}
