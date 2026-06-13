import SwiftUI
import Shared

private let historyWindowDays: Int = 30

private enum ActivityTab: Int, CaseIterable {
    case plan = 0
    case history = 1

    var title: String {
        switch self {
        case .plan: return "План активностей"
        case .history: return "История"
        }
    }
}

struct ActivitiesView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var tabBarState: TabBarState
    @StateObject private var viewModel = ActivitiesViewModelWrapper()

    @State private var selectedTab: ActivityTab = .plan
    @State private var selectedHistoryDate: String? = nil
    @State private var showingDatePicker: Bool = false
    @State private var scheduleTarget: ActivityTodayResponse? = nil
    @State private var showCatalog: Bool = false

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()

            VStack(spacing: 0) {
                navigationBar

                ActivityTabSelector(selectedTab: selectedTab) { tab in
                    selectedTab = tab
                }

                TabView(selection: $selectedTab) {
                    planSection.tag(ActivityTab.plan)
                    historySection.tag(ActivityTab.history)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(isPresented: $showCatalog) {
            ActivityCatalogView()
        }
        .onAppear {
            tabBarState.isVisible = false
            viewModel.load()
            let formatter = isoDateFormatter()
            let today = Date()
            let calendar = Calendar.current
            let start = calendar.date(byAdding: .day, value: -(historyWindowDays - 1), to: today) ?? today
            viewModel.loadHistory(
                startDate: formatter.string(from: start),
                endDate: formatter.string(from: today)
            )
            if selectedHistoryDate == nil {
                selectedHistoryDate = formatter.string(from: today)
            }
        }
        .onChange(of: viewModel.history) { newHistory in
            if selectedHistoryDate == nil {
                if let lastDate = newHistory.last?.date {
                    selectedHistoryDate = lastDate
                } else {
                    let fmt = isoDateFormatter()
                    selectedHistoryDate = fmt.string(from: Date())
                }
            }
        }
        .onDisappear {
            if !showCatalog {
                tabBarState.isVisible = true
            }
        }
    }

    private var navigationBar: some View {
        GymGenieToolbar(
            title: "Активности",
            showBackNavigation: true,
            onBackTap: { dismiss() },
            actions: [
                ToolbarAction(
                    content: AnyView(
                        Text("+")
                            .font(.system(size: 18, weight: .bold))
                    ),
                    action: { showCatalog = true }
                )
            ]
        )
    }

    @ViewBuilder
    private var planSection: some View {
        if viewModel.isLoading {
            spinner
        } else if let error = viewModel.error, viewModel.todayActivities.isEmpty {
            ErrorStateCard(message: error, onRetry: { viewModel.load() })
        } else {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 12) {
                    activityDatePicker

                    if viewModel.todayActivities.isEmpty {
                        EmptyStateCard(
                            icon: "list.bullet.clipboard",
                            message: "Нет активностей на этот день",
                            hint: "Добавь активности через каталог →"
                        )
                    } else {
                        ActivityRowsCard(
                            activities: viewModel.todayActivities,
                            onCheckIn: { id, value in viewModel.checkIn(activityId: id, value: value) },
                            onOpenScheduleSettings: { activity in
                                scheduleTarget = activity
                            },
                            onRemoveFromPlan: { activityId in
                                viewModel.removeFromPlan(activityId: activityId)
                            }
                        )
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
            }
            .safeAreaInset(edge: .bottom) {
                Color.clear.frame(height: 24)
            }
            .sheet(item: $scheduleTarget) { activity in
                ActivityScheduleSettingsView(
                    activityId: activity.activityId,
                    activityName: activity.name,
                    initialScheduleType: activity.scheduleType,
                    initialScheduleDays: activity.scheduleDays as [String],
                    initialOneOffDate: activity.oneOffDate,
                    viewModel: viewModel,
                    onDismiss: { scheduleTarget = nil }
                )
            }
        }
    }

    private var activityDatePicker: some View {
        let formatter = isoDateFormatter()
        let selectedDateObj = formatter.date(from: viewModel.selectedDate) ?? Date()
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let selected = calendar.startOfDay(for: selectedDateObj)

        let label: String = {
            if calendar.isDate(selected, inSameDayAs: today) { return "Сегодня" }
            if let yesterday = calendar.date(byAdding: .day, value: -1, to: today),
               calendar.isDate(selected, inSameDayAs: yesterday) { return "Вчера" }
            if let tomorrow = calendar.date(byAdding: .day, value: 1, to: today),
               calendar.isDate(selected, inSameDayAs: tomorrow) { return "Завтра" }
            let df = DateFormatter()
            df.locale = Locale(identifier: "ru_RU")
            df.dateFormat = "d MMMM"
            return df.string(from: selectedDateObj)
        }()

        return HStack {
            Button(action: {
                if let prev = calendar.date(byAdding: .day, value: -1, to: selectedDateObj) {
                    viewModel.selectDate(date: formatter.string(from: prev))
                }
            }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Color(red: 0.957, green: 0.957, blue: 0.965)))
            }
            .buttonStyle(.plain)

            Spacer()

            Button(action: { showingDatePicker = true }) {
                Text(label)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
            }
            .buttonStyle(.plain)
            .sheet(isPresented: $showingDatePicker) {
                DatePickerSheet(
                    selectedDate: selectedDateObj,
                    onSelect: { date in
                        viewModel.selectDate(date: formatter.string(from: date))
                        showingDatePicker = false
                    }
                )
            }

            Spacer()

            Button(action: {
                if let next = calendar.date(byAdding: .day, value: 1, to: selectedDateObj) {
                    viewModel.selectDate(date: formatter.string(from: next))
                }
            }) {
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Color(red: 0.957, green: 0.957, blue: 0.965)))
            }
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private var historySection: some View {
        if viewModel.isHistoryLoading && viewModel.history.isEmpty {
            spinner
        } else {
            let selectedDay = viewModel.history.first(where: { $0.date == selectedHistoryDate })
            let historyActivities: [ActivityTodayResponse] = viewModel.todayActivities.map { activity in
                let historicalLog = selectedDay?.logs.first(where: { $0.activityId == activity.activityId })
                let logValue: Int32 = historicalLog?.value ?? 0
                return ActivityTodayResponse(
                    activityId: activity.activityId,
                    name: activity.name,
                    ring: activity.ring,
                    kind: activity.kind,
                    presets: activity.presets,
                    unit: activity.unit,
                    goal: activity.goal,
                    inverse: activity.inverse,
                    logValue: logValue,
                    scheduleType: activity.scheduleType,
                    scheduleDays: activity.scheduleDays,
                    oneOffDate: activity.oneOffDate
                )
            }

            ScrollView(showsIndicators: false) {
                VStack(spacing: 12) {
                    ActivityRingsCard(activities: historyActivities)

                    DayStrip(
                        history: historyStripDays,
                        selectedDate: selectedHistoryDate,
                        onSelect: { selectedHistoryDate = $0 }
                    )

                    if let day = selectedDay {

                        HistoryProgressBar(completionPct: day.completionPct)

                        HistoryActivityList(
                            day: day,
                            allActivities: viewModel.todayActivities
                        )
                    } else {
                        HistoryProgressBar(completionPct: 0)

                        Text("Здесь пока ничего нет")
                            .font(.system(size: 18, weight: .medium))
                            .foregroundColor(Palette.mutedText)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 8)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
            }
            .safeAreaInset(edge: .bottom) {
                Color.clear.frame(height: 24)
            }
        }
    }

    private var historyStripDays: [ActivityHistoryDayResponse] {
        let last7 = Array(viewModel.history.suffix(7))
        if !last7.isEmpty { return last7 }
        let cal = Calendar.current
        let today = cal.startOfDay(for: Date())
        let fmt = isoDateFormatter()
        return (0..<7).map { offset in
            let date = cal.date(byAdding: .day, value: -(6 - offset), to: today) ?? today
            return ActivityHistoryDayResponse(
                date: fmt.string(from: date),
                completionPct: 0.0,
                logs: []
            )
        }
    }

    private var spinner: some View {
        ProgressView()
            .progressViewStyle(.circular)
            .tint(Palette.accentOrange)
            .frame(maxWidth: .infinity)
            .frame(minHeight: 200)
    }

    private func isoDateFormatter() -> DateFormatter {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .iso8601)
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }
}

private struct ActivityTabSelector: View {
    let selectedTab: ActivityTab
    let onTabSelected: (ActivityTab) -> Void

    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)
    private let pillBg = Color(red: 0.929, green: 0.925, blue: 0.918)

    var body: some View {
        HStack(spacing: 3) {
            ForEach(ActivityTab.allCases, id: \.self) { tab in
                let selected = selectedTab == tab
                Button(action: {
                    withAnimation(.easeInOut(duration: 0.2)) { onTabSelected(tab) }
                }) {
                    Text(tab.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(selected ? .white : mutedText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Capsule().fill(selected ? deepInk : Color.clear))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(Capsule().fill(pillBg))
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }
}

private struct DatePickerSheet: View {
    let selectedDate: Date
    let onSelect: (Date) -> Void
    @State private var pickerDate: Date

    init(selectedDate: Date, onSelect: @escaping (Date) -> Void) {
        self.selectedDate = selectedDate
        self.onSelect = onSelect
        self._pickerDate = State(initialValue: selectedDate)
    }

    var body: some View {
        NavigationStack {
            DatePicker("", selection: $pickerDate, displayedComponents: .date)
                .datePickerStyle(.graphical)
                .padding()
                .background(Color.white)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Готово") { onSelect(pickerDate) }
                    }
                }
        }
        .presentationBackground(Color.white)
    }
}

private struct DayStrip: View {
    let history: [ActivityHistoryDayResponse]
    let selectedDate: String?
    let onSelect: (String) -> Void

    var body: some View {
        HStack {
            ForEach(history, id: \.date) { day in
                Button(action: { onSelect(day.date) }) {
                    VStack(spacing: 4) {
                        ZStack {
                            Circle()
                                .fill(day.date == selectedDate ? Palette.ringMind : Palette.softCard)
                            Text("\(percent(day.completionPct))%")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(day.date == selectedDate ? .white : Palette.deepInk)
                        }
                        .frame(width: 46, height: 46)

                        Text(formatStripLabel(day.date))
                            .font(.system(size: 14))
                            .foregroundColor(Palette.deepInk)
                    }
                }
                .buttonStyle(.plain)

                if day.date != history.last?.date {
                    Spacer(minLength: 4)
                }
            }
        }
    }

    private func percent(_ raw: Double) -> Int {
        Int((max(0, min(raw, 1.0)) * 100).rounded())
    }

    private func formatStripLabel(_ isoDate: String) -> String {
        let parser = DateFormatter()
        parser.calendar = Calendar(identifier: .iso8601)
        parser.dateFormat = "yyyy-MM-dd"
        parser.locale = Locale(identifier: "en_US_POSIX")
        guard let date = parser.date(from: isoDate) else { return "?" }
        let formatter = DateFormatter()
        formatter.dateFormat = "dd.MM"
        return formatter.string(from: date)
    }
}

private struct EmptyStateCard: View {
    let icon: String
    let message: String
    let hint: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 32))
                .foregroundColor(Palette.accentOrange.opacity(0.7))
            Text(message)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text(hint)
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(32)
        .background(RoundedRectangle(cornerRadius: 24).fill(Palette.softCard))
    }
}

private struct ErrorStateCard: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 32))
                .foregroundColor(Palette.accentOrange.opacity(0.8))
            Text("Не удалось загрузить")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text(message)
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
            Button(action: onRetry) {
                Text("Повторить")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Palette.accentOrange))
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(32)
        .background(RoundedRectangle(cornerRadius: 24).fill(Palette.softCard))
    }
}

private struct HistoryProgressBar: View {
    let completionPct: Double

    private var clampedPct: CGFloat {
        CGFloat(max(0, min(completionPct, 1.0)))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Выполнение")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Palette.mutedText)
                Spacer()
                Text("\(Int((clampedPct * 100).rounded()))%")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Palette.deepInk)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color(red: 0.933, green: 0.933, blue: 0.945))
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Palette.accentOrange)
                        .frame(width: geo.size.width * clampedPct)
                        .animation(.easeInOut(duration: 0.3), value: clampedPct)
                }
            }
            .frame(height: 12)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 16).fill(Color.white))
    }
}

private func ringColor(for ring: String) -> Color {
    switch ring {
    case "MOVE": return Palette.ringMove
    case "MIND": return Palette.ringMind
    case "LIFE": return Palette.ringLife
    default: return Palette.accentOrange
    }
}

private func activityIconName(for ring: String) -> String {
    switch ring {
    case "MOVE": return "ic_activity_run"
    case "MIND": return "ic_activity_mind"
    default: return "ic_activity_schedule"
    }
}

private struct HistoryActivityList: View {
    let day: ActivityHistoryDayResponse
    let allActivities: [ActivityTodayResponse]

    private var rows: [HistoryActivityRow] {
        let logsByActivityId = Dictionary(grouping: day.logs, by: { $0.activityId })
        var result: [HistoryActivityRow] = []
        var seen = Set<String>()

        for activity in allActivities {
            seen.insert(activity.activityId)
            let logValue = logsByActivityId[activity.activityId]?.first?.value ?? 0
            let goalValue = activity.goal?.intValue ?? 0
            let isDone: Bool = {
                if activity.kind == "BINARY" {
                    return activity.inverse ? logValue == 0 : logValue > 0
                }
                return goalValue > 0 ? Int(logValue) >= Int(goalValue) : logValue > 0
            }()
            result.append(HistoryActivityRow(
                name: activity.name,
                logValue: Int(logValue),
                goalValue: Int(goalValue),
                kind: activity.kind,
                ring: activity.ring,
                isDone: isDone
            ))
        }

        for log in day.logs where !seen.contains(log.activityId) {
            result.append(HistoryActivityRow(
                name: log.activityId,
                logValue: Int(log.value),
                goalValue: 0,
                kind: "COUNTER",
                ring: "MOVE",
                isDone: log.value > 0
            ))
        }

        return result
    }

    var body: some View {
        if rows.isEmpty {
            Text("Нет активностей за этот день")
                .font(.system(size: 14))
                .foregroundColor(Palette.mutedText)
                .frame(maxWidth: .infinity)
                .padding(24)
                .background(RoundedRectangle(cornerRadius: 16).fill(Palette.softCard))
        } else {
            VStack(spacing: 8) {
                ForEach(rows) { row in
                    HStack(spacing: 12) {
                        ZStack {
                            Circle().fill(row.isDone
                                ? ringColor(for: row.ring).opacity(0.15)
                                : Color(red: 0.933, green: 0.933, blue: 0.945)
                            )
                            Image(activityIconName(for: row.ring))
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 16, height: 16)
                        }
                        .frame(width: 28, height: 28)

                        Text(row.name)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(Palette.deepInk)
                            .lineLimit(1)

                        Spacer()

                        if row.kind == "BINARY" {
                            Text(row.isDone ? "Выполнено" : "—")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(row.isDone ? Palette.ringMind : Palette.mutedText)
                        } else {
                            Text(row.goalValue > 0
                                ? "\(row.logValue)/\(row.goalValue)"
                                : "\(row.logValue)"
                            )
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(row.isDone ? Palette.deepInk : Palette.mutedText)
                        }
                    }
                    .padding(14)
                    .background(RoundedRectangle(cornerRadius: 14).fill(Color.white))
                }
            }
        }
    }
}

private struct HistoryActivityRow: Identifiable {
    let id = UUID()
    let name: String
    let logValue: Int
    let goalValue: Int
    let kind: String
    let ring: String
    let isDone: Bool
}
