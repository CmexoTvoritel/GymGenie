import SwiftUI

enum MainTab: Int, CaseIterable {
    case home
    case workouts
    case aiCoach
    case stats
    case profile

    var title: String {
        switch self {
        case .home: return "Главная"
        case .workouts: return "Тренировки"
        case .aiCoach: return "ИИ"
        case .stats: return "Статистика"
        case .profile: return "Профиль"
        }
    }

    var icon: String {
        switch self {
        case .home: return "house"
        case .workouts: return "dumbbell"
        case .aiCoach: return "sparkles"
        case .stats: return "chart.bar"
        case .profile: return "person"
        }
    }

    var selectedIcon: String {
        switch self {
        case .home: return "house.fill"
        case .workouts: return "dumbbell.fill"
        case .aiCoach: return "sparkles"
        case .stats: return "chart.bar.fill"
        case .profile: return "person.fill"
        }
    }
}

struct MainView: View {
    @State private var selectedTab: MainTab = .home

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)

    var body: some View {
        ZStack(alignment: .bottom) {
            // Content
            Group {
                switch selectedTab {
                case .home:
                    HomeView()
                case .workouts:
                    WorkoutsView()
                case .aiCoach:
                    PlaceholderView(title: "ИИ Тренер", icon: "sparkles", subtitle: "Скоро будет доступен")
                case .stats:
                    PlaceholderView(title: "Статистика", icon: "chart.bar", subtitle: "Скоро будет доступна")
                case .profile:
                    PlaceholderView(title: "Профиль", icon: "person.circle", subtitle: "Скоро будет доступен")
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.bottom, 80)

            // Custom floating tab bar
            customTabBar
        }
        .background(backgroundColor)
        .edgesIgnoringSafeArea(.bottom)
    }

    // MARK: - Custom Tab Bar

    private var customTabBar: some View {
        HStack(spacing: 0) {
            ForEach(MainTab.allCases, id: \.rawValue) { tab in
                if tab == .aiCoach {
                    // Center elevated AI button
                    Button(action: { selectedTab = tab }) {
                        ZStack {
                            Circle()
                                .fill(
                                    LinearGradient(
                                        gradient: Gradient(colors: [accentColor, accentColor.opacity(0.8)]),
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .frame(width: 56, height: 56)
                                .shadow(color: accentColor.opacity(0.4), radius: 8, y: 4)

                            Image(systemName: tab.icon)
                                .font(.system(size: 22, weight: .semibold))
                                .foregroundColor(.white)
                        }
                        .offset(y: -16)
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    // Regular tab item
                    Button(action: { selectedTab = tab }) {
                        VStack(spacing: 4) {
                            Image(systemName: selectedTab == tab ? tab.selectedIcon : tab.icon)
                                .font(.system(size: 20))
                                .foregroundColor(selectedTab == tab ? accentColor : .gray)

                            Text(tab.title)
                                .font(.system(size: 10, weight: .medium))
                                .foregroundColor(selectedTab == tab ? accentColor : .gray)
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .padding(.horizontal, 8)
        .padding(.top, 12)
        .padding(.bottom, 24)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(.white)
                .shadow(color: Color.black.opacity(0.08), radius: 12, y: -4)
        )
    }
}

// MARK: - Placeholder View

struct PlaceholderView: View {
    let title: String
    let icon: String
    let subtitle: String

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)

    var body: some View {
        VStack(spacing: 16) {
            Spacer()

            Image(systemName: icon)
                .font(.system(size: 48))
                .foregroundColor(accentColor.opacity(0.5))

            Text(title)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.180))

            Text(subtitle)
                .font(.system(size: 15))
                .foregroundColor(.gray)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
