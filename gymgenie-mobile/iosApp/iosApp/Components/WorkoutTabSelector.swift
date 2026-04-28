import SwiftUI
import Shared

struct WorkoutTabSelector: View {
    let selectedTab: Shared.WorkoutsTab
    let onTabSelected: (Shared.WorkoutsTab) -> Void

    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)
    private let pillBg = Color(red: 0.929, green: 0.925, blue: 0.918) // #EDECEA

    var body: some View {
        HStack(spacing: 3) {
            tabButton(title: "Мои планы", tab: .workouts)
            tabButton(title: "Каталог", tab: .exercises)
        }
        .padding(3)
        .background(Capsule().fill(pillBg))
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    private func tabButton(title: String, tab: Shared.WorkoutsTab) -> some View {
        let selected = selectedTab == tab
        return Button(action: {
            withAnimation(.easeInOut(duration: 0.2)) { onTabSelected(tab) }
        }) {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(selected ? .white : mutedText)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(Capsule().fill(selected ? deepInk : Color.clear))
        }
        .buttonStyle(.plain)
    }
}
