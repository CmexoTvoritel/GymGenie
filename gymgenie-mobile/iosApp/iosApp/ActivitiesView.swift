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
        }
        .onChange(of: viewModel.history) { newHistory in
            if selectedHistoryDate == nil {
                selectedHistoryDate = newHistory.last?.date
            }
        }
        .onDisappear {
            tabBarState.isVisible = true
        }
    }

    private var navigationBar: some View {
        HStack {
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Palette.deepInk)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Palette.softCard))
            }

            Spacer()

            Text("Активности")
                .font(.system(size: 17, weight: .heavy))
                .foregroundColor(Palette.deepInk)

            Spacer()

            NavigationLink {
                ActivityCatalogView()
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Palette.accentOrange))
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
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
                            }
                        )
                    }
                    Spacer().frame(height: 24)
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
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
        } else if viewModel.history.isEmpty {
            EmptyStateCard(
                icon: "clock.arrow.circlepath",
                message: "История пуста",
                hint: "Начни отмечать активности — они появятся здесь"
            )
        } else {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 12) {
                    HStack(spacing: 8) {
                        StatCard(value: "\(avgCompletion)%", label: "Среднее", emoji: "📊")
                        StatCard(value: "\(totalLogs)", label: "Записей", emoji: "✅")
                        StatCard(value: "\(perfectDays)", label: "100% дней", emoji: "🏆")
                    }

                    DayStrip(
                        history: Array(viewModel.history.suffix(7)),
                        selectedDate: selectedHistoryDate,
                        onSelect: { selectedHistoryDate = $0 }
                    )

                    if let day = viewModel.history.first(where: { $0.date == selectedHistoryDate }) {
                        if day.logs.isEmpty {
                            Text("Нет записей за этот день")
                                .font(.system(size: 14))
                                .foregroundColor(Palette.mutedText)
                                .frame(maxWidth: .infinity)
                                .padding(24)
                                .background(RoundedRectangle(cornerRadius: 16).fill(Palette.softCard))
                        } else {
                            VStack(spacing: 8) {
                                ForEach(day.logs, id: \.activityId) { log in
                                    LogRow(log: log, activities: viewModel.todayActivities)
                                }
                            }
                        }
                    }

                    Spacer().frame(height: 24)
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
            }
        }
    }

    private var avgCompletion: Int {
        guard !viewModel.history.isEmpty else { return 0 }
        return Int((viewModel.history.map { $0.completionPct }.reduce(0, +) / Double(viewModel.history.count) * 100).rounded())
    }

    private var totalLogs: Int {
        viewModel.history.reduce(0) { $0 + $1.logs.count }
    }

    private var perfectDays: Int {
        viewModel.history.filter { $0.completionPct >= 1.0 }.count
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
                        .font(.system(size: 14, weight: .semibold))
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
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Готово") { onSelect(pickerDate) }
                    }
                }
        }
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
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(day.date == selectedDate ? .white : Palette.deepInk)
                        }
                        .frame(width: 40, height: 40)

                        Text(formatStripLabel(day.date))
                            .font(.system(size: 10))
                            .foregroundColor(Palette.mutedText)
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

private struct LogRow: View {
    let log: ActivityLogResponse
    let activities: [ActivityTodayResponse]

    private var activityName: String {
        activities.first(where: { $0.activityId == log.activityId })?.name ?? log.activityId
    }

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(Palette.ringMind.opacity(0.15))
                Image(systemName: "checkmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Palette.ringMind)
            }
            .frame(width: 28, height: 28)

            Text(activityName)
                .font(.system(size: 12))
                .foregroundColor(Palette.mutedText)
                .lineLimit(1)
                .truncationMode(.middle)

            Spacer()

            Text("\(log.value)")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(Palette.deepInk)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14).fill(Color.white))
    }
}

private struct StatCard: View {
    let value: String
    let label: String
    let emoji: String

    var body: some View {
        VStack(spacing: 6) {
            Text(emoji).font(.system(size: 22))
            Text(value)
                .font(.system(size: 20, weight: .heavy))
                .foregroundColor(Palette.deepInk)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(Palette.mutedText)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 16).fill(.white)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .strokeBorder(Color(red: 0.929, green: 0.929, blue: 0.937), lineWidth: 1)
        )
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
