import SwiftUI
import Shared

/// Step 4 — the builder screen where the user names the workout, tunes the
/// rest interval, reviews added exercises, and saves.
///
/// All persistent state lives in `CreateWorkoutViewModelWrapper`; this view
/// only owns a local string binding to bridge SwiftUI's `TextField` with the
/// shared ViewModel setter.
struct WorkoutBuilderView: View {
    @ObservedObject var vm: CreateWorkoutViewModelWrapper
    let onBack: () -> Void
    let onAddExercise: () -> Void

    @State private var localName: String = ""

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    private var minRest: Int32 { Int32(Shared.CreateWorkoutLimits.shared.MIN_REST_SECONDS) }
    private var maxRest: Int32 { Int32(Shared.CreateWorkoutLimits.shared.MAX_REST_SECONDS) }

    var body: some View {
        ZStack(alignment: .bottom) {
            warmOffWhite.edgesIgnoringSafeArea(.all)

            VStack(spacing: 0) {
                header

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 16) {
                        nameField
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
            }

            bottomBar
        }
        .onAppear {
            if localName != vm.workoutName { localName = vm.workoutName }
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
                Text("Новая тренировка")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(deepInk)
                Text("Настрой параметры и сохрани")
                    .font(.system(size: 12))
                    .foregroundColor(mutedText)
            }

            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 12)
    }

    // MARK: - Name

    private var nameField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Название тренировки")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(mutedText)

            TextField("Например: Грудь и трицепс", text: $localName)
                .font(.system(size: 15))
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

    // MARK: - Rest card

    private var restCard: some View {
        HStack(spacing: 12) {
            Image(systemName: "clock")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(orange)
                .frame(width: 36, height: 36)
                .background(Circle().fill(orange.opacity(0.12)))

            VStack(alignment: .leading, spacing: 2) {
                Text("Отдых между подходами")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(deepInk)
                Text(restDisplay(seconds: vm.restSeconds))
                    .font(.system(size: 12))
                    .foregroundColor(mutedText)
            }

            Spacer()

            HStack(spacing: 8) {
                smallStepperButton(
                    symbol: "minus",
                    disabled: vm.restSeconds <= minRest,
                    action: { vm.decrementRestSeconds() }
                )
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

    // MARK: - Schedule type

    /// Two capsule buttons inside a rounded surface — keeps visual parity with
    /// the rest card: same horizontal padding, same surface fill, same radius.
    private var scheduleTypeCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Тип тренировки")
                .font(.system(size: 13, weight: .semibold))
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
                .font(.system(size: 14, weight: .semibold))
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

    // MARK: - Schedule days

    private static let weekdayKeys: [String] = [
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
        "FRIDAY", "SATURDAY", "SUNDAY",
    ]
    private static let weekdayLabels: [String] = [
        "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс",
    ]

    /// Day-of-week picker shown only for [WorkoutScheduleType.RECURRING].
    /// Seven equally spaced circular badges using the same `softCard` surface
    /// as the rest of the builder for unselected state.
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

    // MARK: - Exercises

    private var exercisesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Упражнения")
                    .font(.system(size: 14, weight: .semibold))
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
                    }
                }
            }
        }
    }

    private var emptyExercisesState: some View {
        VStack(spacing: 6) {
            Text("🧩").font(.system(size: 36))
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
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 10).fill(softCard)
                    .frame(width: 44, height: 44)
                Text(muscleGroupEmoji(exercise.muscleGroupKey))
                    .font(.system(size: 22))
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(exercise.exerciseNameRu)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(deepInk)
                    .lineLimit(1)
                Text("\(exercise.sets) подх • \(exercise.reps) пов")
                    .font(.system(size: 12))
                    .foregroundColor(mutedText)
            }

            Spacer()

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

    // MARK: - Bottom bar

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
                            .font(.system(size: 15, weight: .semibold))
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
                                .font(.system(size: 16, weight: .bold))
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

    // MARK: - Helpers

    /// Formats rest interval according to the spec:
    /// - `<60`  → "Xс"
    /// - exact minute → "Xм"
    /// - otherwise → "Xм Yс"
    private func restDisplay(seconds: Int32) -> String {
        let s = Int(seconds)
        if s < 60 { return "\(s)с" }
        let minutes = s / 60
        let remainder = s % 60
        if remainder == 0 { return "\(minutes)м" }
        return "\(minutes)м \(remainder)с"
    }
}
