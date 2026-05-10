import SwiftUI
import Shared

// MARK: - Activities screen
//
// Live data is fed by `ActivitiesViewModelWrapper`, which forwards calls to the
// shared KMM `ActivitiesViewModel`. The screen itself owns only transient UI
// state (selected tab, selected history day) — everything else flows top-down
// from the wrapper.

private let historyWindowDays: Int = 7

struct ActivitiesView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel = ActivitiesViewModelWrapper()

    @State private var selectedTab: ActivityTab = .today
    @State private var selectedHistoryDate: String? = nil

    enum ActivityTab { case today, history }

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()

            VStack(spacing: 0) {
                navigationBar
                tabBar

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 12) {
                        switch selectedTab {
                        case .today:
                            todaySection
                        case .history:
                            historySection
                        }
                        Spacer().frame(height: 24)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                }
            }
        }
        .navigationBarHidden(true)
        .onAppear {
            viewModel.load()
            // Today's date is computed in the user's current calendar — the
            // backend treats the parameters as local-time ISO dates, so this
            // matches the same window the user actually experienced.
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
            // Default the strip selection to the most recent loaded day, so
            // the logs list does not stay empty after the first load.
            if selectedHistoryDate == nil {
                selectedHistoryDate = newHistory.last?.date
            }
        }
    }

    // MARK: - Navigation bar

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

    // MARK: - Tab bar

    private var tabBar: some View {
        HStack(spacing: 24) {
            tabButton(title: "Сегодня", tab: .today)
            tabButton(title: "История", tab: .history)
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
    }

    private func tabButton(title: String, tab: ActivityTab) -> some View {
        Button(action: { selectedTab = tab }) {
            VStack(spacing: 8) {
                Text(title)
                    .font(.system(size: 15, weight: selectedTab == tab ? .bold : .medium))
                    .foregroundColor(selectedTab == tab ? Palette.deepInk : Palette.mutedText)
                Rectangle()
                    .fill(selectedTab == tab ? Palette.accentOrange : Color.clear)
                    .frame(height: 2)
                    .cornerRadius(1)
            }
        }
    }

    // MARK: - Today

    @ViewBuilder
    private var todaySection: some View {
        if viewModel.isLoading {
            spinner
        } else if let error = viewModel.error, viewModel.todayActivities.isEmpty {
            ErrorStateCard(message: error, onRetry: { viewModel.load() })
        } else if viewModel.todayActivities.isEmpty {
            EmptyStateCard(
                icon: "list.bullet.clipboard",
                message: "Нет активностей на сегодня",
                hint: "Добавь активности через каталог →"
            )
        } else {
            // `ActivityRowsCard` (defined in HomeView) already owns filter
            // chips + the preset bottom sheet, so we drop it in directly.
            ActivityRowsCard(
                activities: viewModel.todayActivities,
                onCheckIn: { id, value in viewModel.checkIn(activityId: id, value: value) }
            )
        }
    }

    // MARK: - History

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
            VStack(spacing: 12) {
                DayStrip(
                    history: viewModel.history,
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
                                LogRow(log: log)
                            }
                        }
                    }
                }
            }
        }
    }

    // MARK: - Helpers

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

// MARK: - Day strip

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

// MARK: - Log row

private struct LogRow: View {
    let log: ActivityLogResponse

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(Palette.ringMind.opacity(0.15))
                Image(systemName: "checkmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(Palette.ringMind)
            }
            .frame(width: 28, height: 28)

            // Without a join to activity names on the client side we keep this
            // as a stable id label — the value column is the actionable signal.
            Text(log.activityId)
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

// MARK: - State cards

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
