import SwiftUI

enum MainTab: Int, CaseIterable {
    case home
    case aiCoach
    case workouts
    case profile

    var title: String {
        switch self {
        case .home: return "Главная"
        case .aiCoach: return "ИИ"
        case .workouts: return "Тренировки"
        case .profile: return "Профиль"
        }
    }

    var icon: String {
        switch self {
        case .home: return "house"
        case .aiCoach: return "sparkles"
        case .workouts: return "dumbbell"
        case .profile: return "person"
        }
    }

    var selectedIcon: String {
        switch self {
        case .home: return "house.fill"
        case .aiCoach: return "sparkles"
        case .workouts: return "dumbbell.fill"
        case .profile: return "person.fill"
        }
    }
}

struct MainView: View {
    @State private var selectedTab: MainTab = .home

    private var navItems: [BottomNavBar.Item] {
        MainTab.allCases.map { tab in
            BottomNavBar.Item(
                id: tab.rawValue,
                title: tab.title,
                icon: tab.icon,
                selectedIcon: tab.selectedIcon
            )
        }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Group {
                switch selectedTab {
                case .home:
                    HomeView()
                case .aiCoach:
                    PlaceholderView(title: "ИИ Тренер", icon: "sparkles", subtitle: "Скоро будет доступен")
                case .workouts:
                    WorkoutsView()
                case .profile:
                    ProfileView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.bottom, 88)

            BottomNavBar(
                items: navItems,
                selectedIndex: selectedTab.rawValue,
                onItemSelected: { index in
                    if let next = MainTab(rawValue: index) {
                        selectedTab = next
                    }
                }
            )
            .padding(.top, 8)
        }
        .background(Palette.warmOffWhite)
    }
}

// MARK: - Placeholder view

struct PlaceholderView: View {
    let title: String
    let icon: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 16) {
            Spacer()

            Image(systemName: icon)
                .font(.system(size: 48))
                .foregroundColor(Palette.accentOrange.opacity(0.55))

            Text(title)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(Palette.deepInk)

            Text(subtitle)
                .font(.system(size: 15))
                .foregroundColor(Palette.mutedText)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
