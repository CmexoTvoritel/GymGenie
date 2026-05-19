import SwiftUI
import Shared

/// View / edit screen for a single workout plan.
///
/// Mirrors the responsibilities of Android's `WorkoutDetailScreen`: shows a
/// read-only summary of the plan, switches into an editable draft when the
/// user taps "Редактировать", and surfaces the existing `ExerciseConfigView`
/// as a full-screen edit surface for individual rows.
///
/// The shared `WorkoutDetailViewModel` owns the load → display → edit → save
/// cycle and validates inputs. This view never mutates the plan directly — it
/// always goes through the wrapper.
struct WorkoutDetailView: View {
    let planId: String
    let onBack: () -> Void
    var onStartPlan: ((String, String) -> Void)? = nil

    @StateObject private var vm: WorkoutDetailViewModelWrapper

    /// Index of the row currently open in the edit overlay. `nil` means the
    /// overlay is hidden. We capture the index (not the `PendingExercise`) so
    /// the live wrapper remains the source of truth — the overlay rebuilds
    /// from the wrapper on every render and gracefully closes if the index
    /// becomes stale (e.g. concurrent delete).
    @State private var editingExerciseIndex: Int? = nil

    @State private var showDeleteConfirm: Bool = false
    @State private var showEditDismissAlert: Bool = false
    @State private var localEditName: String = ""
    @State private var localEditDescription: String = ""
    @State private var dragFromIndex: Int? = nil
    @State private var dragOffset: CGFloat = 0
    @State private var editRowHeight: CGFloat = 68

    init(
        planId: String,
        onBack: @escaping () -> Void,
        onStartPlan: ((String, String) -> Void)? = nil
    ) {
        self.planId = planId
        self.onBack = onBack
        self.onStartPlan = onStartPlan
        _vm = StateObject(wrappedValue: WorkoutDetailViewModelWrapper(planId: planId))
    }

    private let danger = Color(red: 0.898, green: 0.282, blue: 0.302)
    private let inkBlack = Color(red: 0.039, green: 0.039, blue: 0.039)
    private let inkMuted = Color(red: 0.545, green: 0.545, blue: 0.573)
    private let borderGray = Color(red: 0.929, green: 0.929, blue: 0.937)
    private let softGray = Color(red: 0.957, green: 0.957, blue: 0.965)
    private let whiteBg = Color.white
    private let coralTint = Color(red: 1.0, green: 0.957, blue: 0.941)

    var body: some View {
        ZStack {
            content
                .onChange(of: vm.isEditing) { newValue in
                    // Prime local TextField bindings when edit mode starts.
                    if newValue { syncLocalDraftFromWrapper() }
                }
                .onChange(of: vm.isDeleted) { deleted in
                    if deleted { onBack() }
                }
                .onChange(of: vm.isSaved) { saved in
                    // Consume the saved flag once after a successful update so
                    // the wrapper does not redeliver the same notification on
                    // subsequent state ticks.
                    if saved { vm.consumeSavedFlag() }
                }

            if let idx = editingExerciseIndex,
               vm.isEditing,
               vm.editExercises.indices.contains(idx) {
                exerciseEditOverlay(index: idx)
                    .transition(.opacity)
            } else if editingExerciseIndex != nil {
                // Stale index after concurrent delete or after the user left
                // edit mode — close silently so we never present a half-broken
                // overlay against an empty draft.
                Color.clear.onAppear { editingExerciseIndex = nil }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .interactiveDismissDisabled(vm.isEditing)
    }

    @ViewBuilder
    private var content: some View {
        if vm.isLoading && vm.plan == nil {
            loadingView
        } else if let error = vm.errorMessage, vm.plan == nil {
            errorView(message: error)
        } else if vm.plan != nil {
            if vm.isEditing {
                editMode
            } else {
                viewMode
            }
        } else {
            loadingView
        }
    }

    // MARK: - Loading / error placeholders

    private var loadingView: some View {
        VStack {
            Spacer()
            ProgressView().scaleEffect(1.2).tint(Palette.coral)
            Spacer()
        }
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 12) {
            Text("⚠️").font(.system(size: 40))
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(inkMuted)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Button(action: vm.retry) {
                Text("Повторить")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(Palette.coral))
            }
            .buttonStyle(.plain)
            Button(action: onBack) {
                Text("Назад")
                    .font(.system(size: 14))
                    .foregroundColor(inkMuted)
                    .padding(8)
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - View mode

    private var viewMode: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Просмотр",
                showBackNavigation: true,
                onBackTap: onBack,
                actions: [
                    ToolbarAction(
                        content: AnyView(
                            Group {
                                if vm.isDeleting {
                                    ProgressView().tint(danger)
                                        .frame(width: 18, height: 18)
                                } else {
                                    Image(systemName: "trash")
                                        .font(.system(size: 16, weight: .semibold))
                                        .foregroundColor(danger)
                                }
                            }
                        ),
                        action: { if !vm.isDeleting { showDeleteConfirm = true } }
                    )
                ]
            )

            ScrollView(showsIndicators: false) {
                VStack(spacing: 14) {
                    if let plan = vm.plan {
                        heroCard(plan: plan)
                        scheduleCard(plan: plan)
                        restCard(seconds: restSecondsOf(plan: plan))
                        sectionHeader(text: "УПРАЖНЕНИЯ · \(exerciseCountOf(plan: plan))")
                        exerciseListView(plan: plan)
                    }
                    Color.clear.frame(height: 8)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
            }

            viewModeBottomBar
        }
        .alert(
            "Удалить план?",
            isPresented: $showDeleteConfirm,
            actions: {
                Button("Удалить", role: .destructive) { vm.deletePlan() }
                Button("Отмена", role: .cancel) { }
            },
            message: { Text("План тренировок будет удалён без возможности восстановления.") }
        )
    }

    private func heroCard(plan: Shared.WorkoutPlanResponse) -> some View {
        let exerciseCount = exerciseCountOf(plan: plan)
        let totalSets = totalSetsOf(plan: plan)
        let approxMinutes = estimatedMinutesFromExercises(plan: plan)
        return VStack(alignment: .leading, spacing: 0) {
            Text(plan.name)
                .font(.system(size: 25, weight: .heavy))
                .foregroundColor(inkBlack)

            if let desc = plan.description_, !desc.trimmingCharacters(in: .whitespaces).isEmpty {
                Spacer().frame(height: 6)
                Text(desc)
                    .font(.system(size: 16))
                    .foregroundColor(Color(red: 0.333, green: 0.333, blue: 0.376))
                    .lineSpacing(3)
            }

            Spacer().frame(height: 18)

            heroStatsRow(approxMinutes: approxMinutes, exerciseCount: exerciseCount, totalSets: totalSets)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 24).fill(softGray))
        .overlay(
            RoundedRectangle(cornerRadius: 24).stroke(borderGray, lineWidth: 1.5)
        )
    }

    private func heroStatsRow(approxMinutes: Int, exerciseCount: Int, totalSets: Int) -> some View {
        HStack(spacing: 0) {
            statCell(value: "~\(approxMinutes)", unit: "мин", caption: "Время")
            statDivider
            statCell(value: "\(exerciseCount)", unit: "", caption: "Упражнений")
            statDivider
            statCell(value: "\(totalSets)", unit: "", caption: "Подходов")
        }
        .background(RoundedRectangle(cornerRadius: 16).fill(whiteBg))
        .overlay(
            RoundedRectangle(cornerRadius: 16).stroke(borderGray, lineWidth: 1)
        )
    }

    private func statCell(value: String, unit: String, caption: String) -> some View {
        VStack(spacing: 2) {
            HStack(alignment: .bottom, spacing: 2) {
                Text(value)
                    .font(.system(size: 22, weight: .heavy))
                    .foregroundColor(inkBlack)
                if !unit.isEmpty {
                    Text(unit)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(inkMuted)
                        .padding(.bottom, 4)
                }
            }
            Text(caption).font(.system(size: 13)).foregroundColor(inkMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
    }

    private var statDivider: some View {
        Rectangle().fill(borderGray).frame(width: 1, height: 40)
    }

    private func scheduleCard(plan: Shared.WorkoutPlanResponse) -> some View {
        let isRecurring = plan.scheduleType.uppercased() == "RECURRING"
        let activeDays = Set(plan.days.map { $0.dayOfWeek.uppercased() })
        return VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12).fill(coralTint).frame(width: 36, height: 36)
                    Image(systemName: isRecurring ? "repeat" : "calendar")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Palette.coral)
                }
                VStack(alignment: .leading, spacing: 0) {
                    Text("Расписание").font(.system(size: 15)).foregroundColor(inkMuted)
                    Text(isRecurring ? "Постоянная" : "Разовая тренировка")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(inkBlack)
                }
                Spacer()
            }

            Spacer().frame(height: 14)

            if isRecurring {
                HStack(spacing: 6) {
                    ForEach(Self.weekdays, id: \.0) { key, label in
                        let isActive = activeDays.contains(key)
                        ZStack {
                            Circle().fill(isActive ? Palette.coral : softGray)
                            Text(label)
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(isActive ? .white : inkMuted)
                        }
                        .aspectRatio(1, contentMode: .fit)
                    }
                }
            } else {
                Text("Можно выполнить в любой день")
                    .font(.system(size: 17))
                    .foregroundColor(inkMuted)
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 20).fill(whiteBg))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(borderGray, lineWidth: 1))
    }

    private func restCard(seconds: Int) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12).fill(coralTint).frame(width: 36, height: 36)
                Image(systemName: "clock")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Palette.coral)
            }
            VStack(alignment: .leading, spacing: 0) {
                Text("Отдых между подходами").font(.system(size: 15)).foregroundColor(inkMuted)
                Text(formatRestDurationLong(seconds: seconds))
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(inkBlack)
            }
            Spacer()
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 14)
        .background(RoundedRectangle(cornerRadius: 20).fill(whiteBg))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(borderGray, lineWidth: 1))
    }

    private func sectionHeader(text: String) -> some View {
        HStack {
            Text(text)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(inkMuted)
                .kerning(0.7)
            Spacer()
        }
    }

    private func exerciseListView(plan: Shared.WorkoutPlanResponse) -> some View {
        let rows = collectExercises(plan: plan)
        return Group {
            if rows.isEmpty {
                emptyExercisesPlaceholder
            } else {
                VStack(spacing: 10) {
                    ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                        exerciseRowView(index: index + 1, row: row)
                    }
                }
            }
        }
    }

    private var emptyExercisesPlaceholder: some View {
        Text("Упражнений пока нет")
            .font(.system(size: 13))
            .foregroundColor(inkMuted)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .padding(.horizontal, 20)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(borderGray, lineWidth: 1.5)
            )
    }

    private func exerciseRowView(index: Int, row: ExerciseLineItem) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12).fill(softGray).frame(width: 42, height: 42)
                Text(muscleGroupEmoji(row.muscleGroup)).font(.system(size: 18))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(row.name)
                    .font(.system(size: 16.5, weight: .bold))
                    .foregroundColor(inkBlack)
                    .lineLimit(2)
                Text(row.subtitle)
                    .font(.system(size: 14.5))
                    .foregroundColor(inkMuted)
            }
            Spacer()
            ZStack {
                Circle().fill(Palette.coral).frame(width: 32, height: 32)
                Text("\(index)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 16).fill(whiteBg))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(borderGray, lineWidth: 1))
    }

    private var viewModeBottomBar: some View {
        HStack(spacing: 10) {
            Button(action: vm.startEditing) {
                HStack(spacing: 6) {
                    Image(systemName: "pencil")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(inkBlack)
                    Text("Редактировать")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(inkBlack)
                }
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(RoundedRectangle(cornerRadius: 14).fill(whiteBg))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(borderGray, lineWidth: 1.5))
            }
            .buttonStyle(.plain)

            Button(action: {
                if let plan = vm.plan {
                    onStartPlan?(plan.id, plan.name)
                }
            }) {
                HStack(spacing: 6) {
                    Image(systemName: "play.fill")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                    Text("Начать")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(RoundedRectangle(cornerRadius: 14).fill(Palette.coral))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(whiteBg)
        .overlay(Rectangle().frame(height: 1).foregroundColor(borderGray), alignment: .top)
    }

    // MARK: - Edit mode

    private var editMode: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Редактирование тренировки",
                showBackNavigation: true,
                useCloseIcon: true,
                onBackTap: { requestEditDismiss() }
            )

            ScrollView(showsIndicators: false) {
                VStack(spacing: 14) {
                    editNameField
                    editDescriptionField
                    scheduleTypeToggle
                    if vm.editScheduleType == .recurring {
                        scheduleDaysPicker
                    }
                    restStepper
                    sectionHeader(text: "УПРАЖНЕНИЯ · \(vm.editExercises.count)")
                    editExerciseList

                    if let err = vm.errorMessage {
                        Text(err)
                            .font(.system(size: 13))
                            .foregroundColor(danger)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                    }

                    Color.clear.frame(height: 8)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
            }

            editBottomBar
        }
        .alert(
            "Завершить редактирование?",
            isPresented: $showEditDismissAlert,
            actions: {
                Button("Да", role: .destructive) { vm.cancelEditing() }
                Button("Нет", role: .cancel) { }
            },
            message: {
                Text("Вы уверены, что хотите закончить редактирование тренировки? Несохранённые изменения будут потеряны.")
            }
        )
    }

    private var editNameField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Название")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(inkMuted)
            TextField("Название тренировки", text: $localEditName)
                .font(.system(size: 16))
                .foregroundColor(inkBlack)
                .padding(14)
                .background(RoundedRectangle(cornerRadius: 14).fill(whiteBg))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(borderGray, lineWidth: 1))
                .onChange(of: localEditName) { newValue in
                    vm.setEditName(newValue)
                }
        }
    }

    private var editDescriptionField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Описание")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(inkMuted)
            TextField("Краткое описание", text: $localEditDescription, axis: .vertical)
                .font(.system(size: 16))
                .foregroundColor(inkBlack)
                .lineLimit(3...6)
                .padding(14)
                .background(RoundedRectangle(cornerRadius: 14).fill(whiteBg))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(borderGray, lineWidth: 1))
                .onChange(of: localEditDescription) { newValue in
                    vm.setEditDescription(newValue)
                }
        }
    }

    private var scheduleTypeToggle: some View {
        HStack(spacing: 10) {
            scheduleTypePill(label: "Разовая", type: .oneTime)
            scheduleTypePill(label: "Постоянная", type: .recurring)
        }
    }

    private func scheduleTypePill(label: String, type: Shared.WorkoutScheduleType) -> some View {
        let isSelected = vm.editScheduleType == type
        return Button(action: { vm.setEditScheduleType(type) }) {
            Text(label)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(isSelected ? .white : inkBlack)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(
                    RoundedRectangle(cornerRadius: 14)
                        .fill(isSelected ? Palette.coral : whiteBg)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(isSelected ? Palette.coral : borderGray, lineWidth: 1.5)
                )
        }
        .buttonStyle(.plain)
    }

    private var scheduleDaysPicker: some View {
        HStack(spacing: 6) {
            ForEach(Self.weekdays, id: \.0) { key, label in
                let isSelected = vm.editScheduleDays.contains(key)
                Button(action: { vm.toggleEditScheduleDay(key) }) {
                    ZStack {
                        Circle().fill(isSelected ? Palette.coral : softGray)
                        Text(label)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(isSelected ? .white : inkMuted)
                    }
                    .aspectRatio(1, contentMode: .fit)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var restStepper: some View {
        let minRest = Int32(Shared.CreateWorkoutLimits.shared.MIN_REST_SECONDS)
        let maxRest = Int32(Shared.CreateWorkoutLimits.shared.MAX_REST_SECONDS)
        return HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 0) {
                Text("Отдых между подходами")
                    .font(.system(size: 15)).foregroundColor(inkMuted)
                Text(formatRestDurationLong(seconds: Int(vm.editRestSeconds)))
                    .font(.system(size: 17, weight: .bold)).foregroundColor(inkBlack)
            }
            Spacer()
            stepperCircle(
                symbol: "minus",
                enabled: vm.editRestSeconds > minRest,
                action: { vm.decrementEditRestSeconds() }
            )
            stepperCircle(
                symbol: "plus",
                enabled: vm.editRestSeconds < maxRest,
                action: { vm.incrementEditRestSeconds() }
            )
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 16).fill(whiteBg))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(borderGray, lineWidth: 1))
    }

    private func stepperCircle(symbol: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 40, height: 40)
                .background(Circle().fill(enabled ? Palette.coral : inkMuted.opacity(0.35)))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private var editExerciseList: some View {
        Group {
            if vm.editExercises.isEmpty {
                emptyExercisesPlaceholder
            } else {
                VStack(spacing: 10) {
                    ForEach(Array(vm.editExercises.enumerated()), id: \.offset) { index, exercise in
                        editExerciseRow(index: index, exercise: exercise)
                            .zIndex(dragFromIndex == index ? 1 : 0)
                            .offset(y: editDragYOffset(for: index))
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
                                            let to = editClampedTargetIndex(from: from, offset: dragOffset)
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
                                    Color.clear.onAppear { editRowHeight = geo.size.height + 10 }
                                }
                            )
                    }
                }
            }
        }
    }

    /// Row in edit mode: emoji + name + "sets × reps" subtitle, with a pencil
    /// edit button and a delete button on the trailing edge. Matches the
    /// create-workout builder's exercise row exactly so the two flows feel
    /// like the same surface.
    private func editExerciseRow(index: Int, exercise: Shared.PendingExercise) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12).fill(softGray).frame(width: 42, height: 42)
                Text(muscleGroupEmoji(exercise.muscleGroupKey)).font(.system(size: 18))
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(exercise.exerciseNameRu)
                    .font(.system(size: 16.5, weight: .bold))
                    .foregroundColor(inkBlack)
                    .lineLimit(2)
                Text(editExerciseSubtitle(exercise))
                    .font(.system(size: 14.5))
                    .foregroundColor(inkMuted)
            }
            Spacer()

            Button(action: { editingExerciseIndex = index }) {
                Image(systemName: "pencil")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(inkBlack)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(softGray))
            }
            .buttonStyle(.plain)

            Spacer().frame(width: 8)

            Button(action: { vm.removeExercise(at: index) }) {
                Image(systemName: "trash")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(danger)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(softGray))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 16).fill(whiteBg))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(borderGray, lineWidth: 1))
    }

    private var editBottomBar: some View {
        let canSave =
            !vm.editName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            !vm.editExercises.isEmpty &&
            !vm.isSaving
        return Button(action: { vm.saveEdit() }) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(canSave ? Palette.coral : Palette.coral.opacity(0.5))
                if vm.isSaving {
                    ProgressView().tint(.white)
                } else {
                    HStack(spacing: 6) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                        Text("Сохранить")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(.white)
                    }
                }
            }
            .frame(height: 52)
        }
        .buttonStyle(.plain)
        .disabled(!canSave)
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(whiteBg)
        .overlay(Rectangle().frame(height: 1).foregroundColor(borderGray), alignment: .top)
    }

    // MARK: - Edit overlay

    private func exerciseEditOverlay(index: Int) -> some View {
        let pending = vm.editExercises[index]
        return ZStack {
            ExerciseConfigView(
                exercise: Self.buildExerciseShortResponse(from: pending),
                onBack: { editingExerciseIndex = nil },
                onConfirm: { updated in
                    vm.updatePendingExerciseAt(index: index, updated: updated)
                    editingExerciseIndex = nil
                },
                prefillFrom: pending,
                showStepHeader: false
            )
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.warmOffWhite.ignoresSafeArea())
    }

    // MARK: - Edit dismiss

    /// Returns `true` when the edit draft differs from the loaded plan.
    private var hasUnsavedEditChanges: Bool {
        guard let plan = vm.plan else { return false }

        if vm.editName != plan.name { return true }
        if vm.editDescription != (plan.description_ ?? "") { return true }

        let originalScheduleType: Shared.WorkoutScheduleType =
            plan.scheduleType.uppercased() == "RECURRING" ? .recurring : .oneTime
        if vm.editScheduleType != originalScheduleType { return true }

        let originalDays = Set(plan.days.map { $0.dayOfWeek.uppercased() })
        if vm.editScheduleDays != originalDays { return true }

        let originalRest = restSecondsOf(plan: plan)
        if Int(vm.editRestSeconds) != originalRest { return true }

        let originalExercises = collectExercises(plan: plan)
        if vm.editExercises.count != originalExercises.count { return true }
        for (i, ex) in vm.editExercises.enumerated() {
            let orig = originalExercises[i]
            if ex.exerciseId != orig.exerciseId ||
               ex.sets != orig.sets ||
               ex.reps != orig.reps {
                return true
            }
            if !weightsEqual(ex.setWeightsKg as? [KotlinDouble?], orig.setWeightsKg) {
                return true
            }
        }
        return false
    }

    /// Structural comparison of two bridged `List<Double?>?` weight arrays.
    /// Both `nil` → equal; one `nil` → not equal; both non-nil → element-wise
    /// comparison via `KotlinDouble.doubleValue`.
    private func weightsEqual(_ lhs: [KotlinDouble?]?, _ rhs: [KotlinDouble?]?) -> Bool {
        switch (lhs, rhs) {
        case (nil, nil):
            return true
        case (.some(let a), .some(let b)):
            guard a.count == b.count else { return false }
            for (l, r) in zip(a, b) {
                switch (l, r) {
                case (nil, nil):
                    continue
                case (.some(let lv), .some(let rv)):
                    if lv.doubleValue != rv.doubleValue { return false }
                default:
                    return false
                }
            }
            return true
        default:
            return false
        }
    }

    private func requestEditDismiss() {
        if hasUnsavedEditChanges {
            showEditDismissAlert = true
        } else {
            vm.cancelEditing()
        }
    }

    // MARK: - Helpers

    private func syncLocalDraftFromWrapper() {
        if localEditName != vm.editName { localEditName = vm.editName }
        if localEditDescription != vm.editDescription { localEditDescription = vm.editDescription }
    }

    // MARK: - Drag reorder helpers

    private func editDragYOffset(for index: Int) -> CGFloat {
        guard let from = dragFromIndex else { return 0 }
        if index == from { return dragOffset }
        let targetIndex = editClampedTargetIndex(from: from, offset: dragOffset)
        if from < targetIndex && index > from && index <= targetIndex {
            return -editRowHeight
        } else if from > targetIndex && index < from && index >= targetIndex {
            return editRowHeight
        }
        return 0
    }

    private func editClampedTargetIndex(from: Int, offset: CGFloat) -> Int {
        let steps = Int(round(offset / editRowHeight))
        let target = from + steps
        return max(0, min(target, vm.editExercises.count - 1))
    }

    /// Rebuilds a minimal `ExerciseShortResponse` from a `PendingExercise`.
    /// The config view reads only id / names / muscleGroup / requiresWeight,
    /// so the catalog-only fields (difficulty, calories, etc.) are left empty.
    private static func buildExerciseShortResponse(from pending: Shared.PendingExercise) -> Shared.ExerciseShortResponse {
        Shared.ExerciseShortResponse(
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
    }

    /// Builds the subtitle for an edit-mode exercise row, including weight
    /// info when available. Mirrors `WorkoutBuilderView.exerciseRowSubtitle`.
    private func editExerciseSubtitle(_ exercise: Shared.PendingExercise) -> String {
        let base = "\(exercise.sets) × \(exercise.reps)"
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

    private static let weekdays: [(String, String)] = [
        ("MONDAY", "Пн"),
        ("TUESDAY", "Вт"),
        ("WEDNESDAY", "Ср"),
        ("THURSDAY", "Чт"),
        ("FRIDAY", "Пт"),
        ("SATURDAY", "Сб"),
        ("SUNDAY", "Вс"),
    ]
}

// MARK: - Pure helpers

private struct ExerciseLineItem {
    let exerciseId: String
    let name: String
    let muscleGroup: String
    let sets: Int32
    let reps: Int32
    /// Bridged from Kotlin `List<Double?>?`. Each element is `KotlinDouble?`.
    let setWeightsKg: [KotlinDouble?]?
    let subtitle: String
}

private func collectExercises(plan: Shared.WorkoutPlanResponse) -> [ExerciseLineItem] {
    plan.days
        .sorted { $0.orderIndex < $1.orderIndex }
        .flatMap { day in
            day.exercises
                .sorted { $0.orderIndex < $1.orderIndex }
                .map { ex in
                    let subtitle = buildViewExerciseSubtitle(
                        sets: ex.sets, reps: ex.reps, setWeightsKg: ex.setWeightsKg as? [KotlinDouble?]
                    )
                    return ExerciseLineItem(
                        exerciseId: ex.exerciseId,
                        name: ex.exerciseNameRu,
                        muscleGroup: ex.muscleGroup,
                        sets: ex.sets,
                        reps: ex.reps,
                        setWeightsKg: ex.setWeightsKg as? [KotlinDouble?],
                        subtitle: subtitle
                    )
                }
        }
}

private func restSecondsOf(plan: Shared.WorkoutPlanResponse) -> Int {
    let perExercise: [Int32] = plan.days.flatMap { $0.exercises.map { $0.restSeconds } }
    if perExercise.isEmpty { return 60 }
    let counts = Dictionary(grouping: perExercise, by: { $0 }).mapValues(\.count)
    let mostCommon = counts.max(by: { $0.value < $1.value })?.key
    return Int(mostCommon ?? perExercise.first ?? 60)
}

private func exerciseCountOf(plan: Shared.WorkoutPlanResponse) -> Int {
    guard let day = plan.days.min(by: { $0.orderIndex < $1.orderIndex }) else { return 0 }
    return day.exercises.count
}

private func totalSetsOf(plan: Shared.WorkoutPlanResponse) -> Int {
    guard let day = plan.days.min(by: { $0.orderIndex < $1.orderIndex }) else { return 0 }
    return day.exercises.reduce(0) { $0 + Int($1.sets) }
}

/// Per-exercise time estimation using `secondsPer10Reps` from the backend.
/// Falls back to 30 seconds when the field is absent (older plans).
private func estimatedMinutesFromExercises(plan: Shared.WorkoutPlanResponse) -> Int {
    guard let day = plan.days.min(by: { $0.orderIndex < $1.orderIndex }) else { return 0 }
    let exercises = day.exercises.sorted { $0.orderIndex < $1.orderIndex }
    if exercises.isEmpty { return 0 }

    var totalSeconds: Double = 0
    for ex in exercises {
        let secPer10 = ex.secondsPer10Reps?.doubleValue ?? 30.0
        let workSeconds = (secPer10 / 10.0) * Double(ex.reps) * Double(ex.sets)
        let restTotal = Double(ex.restSeconds) * Double(max(0, Int(ex.sets) - 1))
        totalSeconds += workSeconds + restTotal
    }
    let minutes = Int(totalSeconds / 60.0)
    return max(1, minutes)
}

/// Builds subtitle for a view-mode exercise row: "sets × reps" with optional
/// weight suffix. Unlike the edit variant, the plan response does not carry a
/// `requiresWeight` flag so we simply check whether concrete weights exist.
private func buildViewExerciseSubtitle(sets: Int32, reps: Int32, setWeightsKg: [KotlinDouble?]?) -> String {
    let base = "\(sets) × \(reps)"
    guard let raw = setWeightsKg else { return base }

    let weights: [Double] = raw.compactMap { $0?.doubleValue }
    guard !weights.isEmpty else { return base }

    func fmt(_ kg: Double) -> String {
        kg.truncatingRemainder(dividingBy: 1) == 0 ? String(Int(kg)) : String(format: "%.1f", kg)
    }

    let unique = Array(Set(weights))
    if unique.count == 1 {
        return "\(base) • \(fmt(unique[0])) кг"
    }
    let minW = weights.min() ?? 0
    let maxW = weights.max() ?? 0
    return "\(base) • \(fmt(minW))-\(fmt(maxW)) кг"
}

private func formatRestDurationLong(seconds: Int) -> String {
    if seconds < 60 { return "\(seconds) сек" }
    let minutes = seconds / 60
    let remainder = seconds % 60
    return remainder == 0 ? "\(minutes) мин" : "\(minutes) мин \(remainder) сек"
}
