import SwiftUI

// MARK: - Activities screen
//
// Pushed onto the navigation stack from HomeView via the "Ещё →" link.
// All rows are hardcoded mock data until a dedicated backend endpoint is added.

struct ActivitiesView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selectedTab: ActivityTab = .today

    enum ActivityTab { case today, history }

    // TODO: backend integration. Replace with an ActivityFeedViewModel when available.
    private let todayRows: [ActivityListRow] = [
        ActivityListRow(emoji: "🚶", title: "Шаги", unit: "шагов", current: 6_420, total: 10_000, color: Palette.activityWalking),
        ActivityListRow(emoji: "💧", title: "Вода", unit: "стаканов", current: 5, total: 8, color: Palette.activityWater),
        ActivityListRow(emoji: "🔥", title: "Калории", unit: "ккал", current: 1_200, total: 2_000, color: Palette.ringMovement),
        ActivityListRow(emoji: "😴", title: "Сон", unit: "часов", current: 7, total: 8, color: Palette.activityMeditation),
        ActivityListRow(emoji: "🧘", title: "Медитация", unit: "минут", current: 0, total: 15, color: Palette.activityStretching),
    ]

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()

            VStack(spacing: 0) {
                navigationBar
                tabBar

                ScrollView(showsIndicators: false) {
                    LazyVStack(spacing: 12) {
                        switch selectedTab {
                        case .today:
                            ForEach(todayRows) { row in
                                ActivityListCardRow(row: row)
                            }
                        case .history:
                            historyPlaceholder
                        }
                        Spacer().frame(height: 24)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                }
            }
        }
        .navigationBarHidden(true)
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

    // MARK: - History placeholder

    private var historyPlaceholder: some View {
        VStack(spacing: 10) {
            Image(systemName: "clock.arrow.circlepath")
                .font(.system(size: 32))
                .foregroundColor(Palette.accentOrange.opacity(0.7))
            Text("История ещё пуста")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Palette.deepInk)
            Text("Отмечай активности — они появятся здесь")
                .font(.system(size: 13))
                .foregroundColor(Palette.mutedText)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(32)
        .background(RoundedRectangle(cornerRadius: 24).fill(Palette.softCard))
    }
}

// MARK: - Model

struct ActivityListRow: Identifiable {
    let id = UUID()
    let emoji: String
    let title: String
    let unit: String
    let current: Double
    let total: Double
    let color: Color

    var percent: Int {
        guard total > 0 else { return 0 }
        return Int((current / total * 100.0).rounded())
    }

    var progress: CGFloat {
        guard total > 0 else { return 0 }
        return CGFloat(min(max(current / total, 0), 1))
    }

    var formattedProgress: String {
        "\(format(current)) / \(format(total)) \(unit)"
    }

    private func format(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = value.truncatingRemainder(dividingBy: 1) == 0 ? 0 : 1
        formatter.groupingSeparator = " "
        return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
    }
}

// MARK: - Row

private struct ActivityListCardRow: View {
    let row: ActivityListRow

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle()
                    .stroke(
                        row.color.opacity(0.25),
                        style: StrokeStyle(lineWidth: 3, dash: [3, 3])
                    )
                Circle()
                    .trim(from: 0, to: row.progress)
                    .stroke(
                        row.color,
                        style: StrokeStyle(lineWidth: 3, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                Text(row.emoji).font(.system(size: 20))
            }
            .frame(width: 48, height: 48)

            VStack(alignment: .leading, spacing: 4) {
                Text(row.title)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Palette.deepInk)
                Text(row.formattedProgress)
                    .font(.system(size: 13))
                    .foregroundColor(Palette.mutedText)
            }

            Spacer()

            Text("\(row.percent)%")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(.white)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Capsule().fill(Palette.accentOrange))
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 6, y: 2)
    }
}
