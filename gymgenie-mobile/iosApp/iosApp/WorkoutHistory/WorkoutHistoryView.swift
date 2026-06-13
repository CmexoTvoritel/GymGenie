import SwiftUI
import Shared

private enum HistoryColors {
    static let black = Color(red: 0.039, green: 0.039, blue: 0.039)
    static let border = Color(red: 0.929, green: 0.929, blue: 0.937)
    static let muted = Color(red: 0.545, green: 0.545, blue: 0.573)
    static let soft = Color(red: 0.957, green: 0.957, blue: 0.965)
    static let coral = Color(red: 1.0, green: 0.353, blue: 0.235)

    static let completedBg = Color(red: 0.902, green: 0.965, blue: 0.925)
    static let completedFg = Color(red: 0.118, green: 0.557, blue: 0.310)
    static let completedAccent = Color(red: 0.133, green: 0.627, blue: 0.420)

    static let incompleteBg = Color(red: 1.0, green: 0.957, blue: 0.855)
    static let incompleteFg = Color(red: 0.541, green: 0.353, blue: 0.0)
    static let incompleteAccent = Color(red: 0.910, green: 0.608, blue: 0.071)
}

private let dayAbbreviations: [Int32: String] = [
    1: "Пн", 2: "Вт", 3: "Ср", 4: "Чт", 5: "Пт", 6: "Сб", 7: "Вс"
]

private let monthNames = [
    "", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
]

struct WorkoutHistoryView: View {
    let onBack: () -> Void

    @StateObject private var viewModel = WorkoutHistoryViewModelWrapper()
    @State private var showDatePicker = false
    @State private var selectedSession: WorkoutSessionHistoryItem? = nil

    private let today: Date = Calendar.current.startOfDay(for: Date())

    private func dismissSessionDetail() {
        selectedSession = nil
    }

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()

            VStack(spacing: 0) {
                GymGenieToolbar(
                    title: "Статистика",
                    showBackNavigation: true,
                    onBackTap: onBack
                )

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 0) {
                        header

                        dateSelector
                            .padding(.bottom, 20)

                        if !completedSessions.isEmpty {
                            daySummary
                                .padding(.bottom, 16)
                        }

                        content
                    }
                }
                .refreshable {
                    await viewModel.refreshAsync()
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showDatePicker) {
            datePickerSheet
        }
        .navigationDestination(isPresented: Binding(
            get: { selectedSession != nil },
            set: { if !$0 { dismissSessionDetail() } }
        )) {
            if let session = selectedSession {
                HistorySummaryView(session: session, onBack: { dismissSessionDetail() })
                    .toolbar(.hidden, for: .navigationBar)
            }
        }
    }

    private var completedSessions: [WorkoutSessionHistoryItem] {
        viewModel.sessions.filter { $0.status == "COMPLETED" }
    }

    private var selectedSwiftDate: Date {
        WorkoutHistoryViewModelWrapper.toSwiftDate(viewModel.selectedDate)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 0) {
            Spacer().frame(height: 4)
            Text("Аналитика твоих тренировок")
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(HistoryColors.black)
            Spacer().frame(height: 12)
        }
        .padding(.horizontal, 20)
    }

    private var dateSelector: some View {
        VStack(alignment: .leading, spacing: 0) {
            let monthNumber = Int(viewModel.selectedDate.monthNumber)
            let monthName = monthNames[safe: monthNumber] ?? ""

            Button(action: { showDatePicker = true }) {
                HStack {
                    Text(dateLabel)
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(HistoryColors.black)
                    Spacer()
                    Text("\(monthName) \(String(viewModel.selectedDate.year))")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(HistoryColors.muted)
                }
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 20)
            .padding(.bottom, 16)

            weekStrip
                .padding(.horizontal, 12)
        }
    }

    private var dateLabel: String {
        let sel = selectedSwiftDate
        if Calendar.current.isDateInToday(sel) { return "Сегодня" }
        if Calendar.current.isDateInYesterday(sel) { return "Вчера" }
        let day = viewModel.selectedDate.dayOfMonth
        let monthNumber = Int(viewModel.selectedDate.monthNumber)
        let monthName = monthNames[safe: monthNumber]?.lowercased() ?? ""
        return "\(day) \(monthName)"
    }

    private var weekStrip: some View {
        HStack(spacing: 4) {
            Button(action: { viewModel.shiftWeek(-1) }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(HistoryColors.black)
                    .frame(width: 32, height: 32)
            }
            .buttonStyle(.plain)

            ForEach(viewModel.weekDates, id: \.self) { date in
                let dateStr = WorkoutHistoryViewModelWrapper.localDateString(date)
                let isSelected = date == viewModel.selectedDate
                let isToday = Calendar.current.isDateInToday(
                    WorkoutHistoryViewModelWrapper.toSwiftDate(date)
                )
                let daySessions = viewModel.weekSessions[dateStr] ?? []
                let hasCompleted = daySessions.contains { $0.status == "COMPLETED" }
                let hasCancelled = daySessions.contains { $0.status == "CANCELLED" }

                weekDayButton(
                    date: date,
                    isSelected: isSelected,
                    isToday: isToday,
                    hasCompleted: hasCompleted,
                    hasCancelled: hasCancelled
                )
            }

            Button(action: { viewModel.shiftWeek(1) }) {
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(HistoryColors.black)
                    .frame(width: 32, height: 32)
            }
            .buttonStyle(.plain)
        }
    }

    private func weekDayButton(
        date: Kotlinx_datetimeLocalDate,
        isSelected: Bool,
        isToday: Bool,
        hasCompleted: Bool,
        hasCancelled: Bool
    ) -> some View {
        let isoDow = Int32(date.dayOfWeek.ordinal) + 1
        let abbr = dayAbbreviations[isoDow] ?? ""
        let swiftDate = WorkoutHistoryViewModelWrapper.toSwiftDate(date)
        let isFuture = swiftDate > today
        let futureGray = Color(red: 0.816, green: 0.816, blue: 0.831)
        let textColor: Color = isFuture ? futureGray : (isSelected ? .white : (isToday ? HistoryColors.coral : HistoryColors.black))

        return Button {
            viewModel.selectDate(date)
        } label: {
            VStack(spacing: 4) {
                Text(abbr)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(isFuture ? futureGray : (isSelected ? Color.white.opacity(0.7) : HistoryColors.muted))

                Text("\(date.dayOfMonth)")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(textColor)

                HStack(spacing: 3) {
                    if hasCompleted {
                        Circle()
                            .fill(isSelected ? Color.white : HistoryColors.completedAccent)
                            .frame(width: 5, height: 5)
                    }
                    if hasCancelled {
                        Circle()
                            .fill(isSelected ? Color.white.opacity(0.6) : HistoryColors.incompleteAccent)
                            .frame(width: 5, height: 5)
                    }
                    if !hasCompleted && !hasCancelled {
                        Color.clear.frame(width: 5, height: 5)
                    }
                }
            }
            .padding(.vertical, 5)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(isFuture ? Color.clear : (isSelected ? HistoryColors.coral : Color.clear))
                    .shadow(
                        color: isSelected && !isFuture ? Color.black.opacity(0.15) : .clear,
                        radius: 4, y: 2
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(isFuture || isSelected ? Color.clear : HistoryColors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .disabled(isFuture)
    }

    private var daySummary: some View {
        let totalMinutes = completedSessions.reduce(0) { $0 + Int(($1.durationMinutes?.int32Value ?? 0)) }
        let totalExercises = completedSessions.reduce(0) { $0 + Int($1.totalExercises) }
        let totalSets = completedSessions.reduce(0) { $0 + Int($1.completedSets) }
        let estimatedCalories = totalExercises * 45 + totalMinutes * 4

        return HStack {
            Spacer()
            summaryColumn(iconName: "ic_timer", label: "Время", value: "\(totalMinutes) мин")
            Spacer()
            summaryColumn(iconName: "ic_repeat", label: "Подходы", value: "\(totalSets)")
            Spacer()
            summaryColumn(iconName: "ic_calories", label: "ккал", value: "\(estimatedCalories)")
            Spacer()
        }
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.white)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(HistoryColors.border, lineWidth: 1)
                )
        )
        .padding(.horizontal, 20)
    }

    private func summaryColumn(iconName: String, label: String, value: String) -> some View {
        VStack(spacing: 4) {
            Image(iconName)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 36, height: 36)
            Text(value)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(HistoryColors.black)
            Text(label)
                .font(.system(size: 16))
                .foregroundColor(HistoryColors.muted)
        }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading {
            HStack {
                Spacer()
                ProgressView()
                    .scaleEffect(1.2)
                    .tint(HistoryColors.coral)
                Spacer()
            }
            .padding(.vertical, 48)
        } else if let error = viewModel.error {
            VStack(spacing: 4) {
                Text("Ошибка загрузки")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(HistoryColors.black)
                Text(error)
                    .font(.system(size: 14))
                    .foregroundColor(HistoryColors.muted)
                    .multilineTextAlignment(.center)
                Button("Повторить") { viewModel.refresh() }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(HistoryColors.coral)
                    .padding(.top, 12)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 48)
            .padding(.horizontal, 20)
        } else if viewModel.sessions.isEmpty {
            emptyState
        } else {
            LazyVStack(spacing: 12) {
                ForEach(viewModel.sessions, id: \.id) { session in
                    historyCard(session: session)
                        .onTapGesture {
                            selectedSession = session
                        }
                }
            }
            .padding(.bottom, 24)
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image("ic_weekend_day")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 100, height: 100)
            Text("День отдыха")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(HistoryColors.black)
            Text("В этот день тренировок не было")
                .font(.system(size: 18))
                .foregroundColor(HistoryColors.muted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
    }

    private func historyCard(session: WorkoutSessionHistoryItem) -> some View {
        let isCompleted = session.status == "COMPLETED"
        let statusBg = isCompleted ? HistoryColors.completedBg : HistoryColors.incompleteBg
        let statusFg = isCompleted ? HistoryColors.completedFg : HistoryColors.incompleteFg
        let statusText = isCompleted ? "Выполнено" : "Не закончено"
        let muscleColors = muscleGroupColorPair(session.primaryMuscleGroup)

        let progress: CGFloat = session.totalSets > 0
            ? CGFloat(session.completedSets) / CGFloat(session.totalSets)
            : (isCompleted ? 1.0 : 0.0)

        return VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(muscleColors.bg)
                        Image(muscleGroupExerciseImageName(session.primaryMuscleGroup ?? ""))
                            .resizable()
                            .aspectRatio(1, contentMode: .fit)
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .frame(width: 48, height: 48)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(session.name)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(HistoryColors.black)
                            .lineLimit(2)

                        if let group = session.primaryMuscleGroup {
                            Text(Self.muscleGroupLabel(group))
                                .font(.system(size: 15))
                                .foregroundColor(HistoryColors.muted)
                        }
                    }

                    Spacer()

                    Text(statusText)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(statusFg)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(
                            Capsule().fill(statusBg)
                        )
                }

                HStack(spacing: 8) {
                    let startTime = Self.formatEpochTime(session.startedAt)
                    if let endTime = (session.finishedAt?.doubleValue).map(Self.formatEpochTime) {
                        infoChip("\(startTime) – \(endTime)")
                    }
                    if let duration = session.durationMinutes?.int32Value {
                        infoChip("\(duration) мин")
                    }
                }

                if progress > 0 {
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(HistoryColors.soft)
                                .frame(height: 6)
                            Capsule()
                                .fill(HistoryColors.coral)
                                .frame(width: geo.size.width * min(max(progress, 0), 1), height: 6)
                        }
                    }
                    .frame(height: 6)
                }

                if isCompleted {
                    HStack(spacing: 16) {
                        statItem(label: "Упражнения", value: "\(session.totalExercises)")
                        statItem(label: "Подходы", value: "\(session.completedSets)")
                    }
                } else {
                    if session.totalExercises > 0 {
                        Text("Упражнения: \(session.completedExercises) из \(session.totalExercises)")
                            .font(.system(size: 16))
                            .foregroundColor(HistoryColors.incompleteFg)
                    }
                    if session.totalSets > 0 {
                        Text("Подходы: \(session.completedSets) из \(session.totalSets)")
                            .font(.system(size: 16))
                            .foregroundColor(HistoryColors.incompleteFg)
                    }
                }
            }
            .padding(16)
        }
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(HistoryColors.border, lineWidth: 1)
        )
        .padding(.horizontal, 20)
    }

    private func infoChip(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 14, weight: .medium))
            .foregroundColor(HistoryColors.muted)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(HistoryColors.soft)
            )
    }

    private func statItem(label: String, value: String) -> some View {
        HStack(spacing: 4) {
            Text(value)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(HistoryColors.black)
            Text(label)
                .font(.system(size: 15))
                .foregroundColor(HistoryColors.muted)
        }
    }

    private var datePickerSheet: some View {
        VStack(spacing: 16) {
            HStack {
                Spacer()
                Button("Готово") {
                    showDatePicker = false
                }
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(HistoryColors.coral)
                .padding(.trailing, 20)
                .padding(.top, 16)
            }

            DatePicker(
                "Выбрать дату",
                selection: Binding(
                    get: { selectedSwiftDate },
                    set: { newDate in
                        viewModel.selectSwiftDate(newDate)
                    }
                ),
                in: ...Date(),
                displayedComponents: .date
            )
            .datePickerStyle(.graphical)
            .padding(.horizontal, 16)

            Spacer()
        }
        .presentationDetents([.medium])
    }

    private static func formatEpochTime(_ epochMillis: Double) -> String {
        let date = Date(timeIntervalSince1970: epochMillis / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }

    private static func muscleGroupLabel(_ group: String) -> String {
        switch group.uppercased() {
        case "CHEST": return "Грудные мышцы"
        case "BACK": return "Спина"
        case "SHOULDERS", "SHOULDER": return "Плечи"
        case "BICEPS": return "Бицепс"
        case "TRICEPS": return "Трицепс"
        case "FOREARMS": return "Предплечья"
        case "ARMS": return "Руки"
        case "ABS", "CORE": return "Пресс"
        case "QUADRICEPS": return "Квадрицепсы"
        case "HAMSTRINGS": return "Задняя поверхность бедра"
        case "GLUTES": return "Ягодицы"
        case "CALVES": return "Икроножные"
        case "LEGS": return "Ноги"
        case "CARDIO": return "Кардио"
        case "FULL_BODY": return "Все тело"
        default: return "Смешанная"
        }
    }
}

extension WorkoutSessionHistoryItem: @retroactive Identifiable {}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
