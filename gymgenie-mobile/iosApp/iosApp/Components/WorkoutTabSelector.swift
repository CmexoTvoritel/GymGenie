import SwiftUI
import Shared

struct WorkoutTabSelector: View {
    let selectedTab: Shared.WorkoutsTab
    let onTabSelected: (Shared.WorkoutsTab) -> Void

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)

    var body: some View {
        HStack(spacing: 0) {
            tabButton(title: "Тренировки", tab: .workouts)
            tabButton(title: "Упражнения", tab: .exercises)
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.gray.opacity(0.1))
        )
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    private func tabButton(title: String, tab: Shared.WorkoutsTab) -> some View {
        Button(action: {
            withAnimation(.easeInOut(duration: 0.2)) {
                onTabSelected(tab)
            }
        }) {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(selectedTab == tab ? .white : .gray)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(selectedTab == tab ? accentColor : Color.clear)
                )
        }
    }
}
