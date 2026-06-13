import SwiftUI

final class TabBarState: ObservableObject {
    @Published var isVisible: Bool = true
    @Published var pendingTabSwitch: MainTab? = nil

    func switchTo(_ tab: MainTab) {
        pendingTabSwitch = tab
    }
}

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
    @StateObject private var profileStore = UserProfileStoreWrapper()
    @StateObject private var tabBarState = TabBarState()

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
        if #available(iOS 26, *) {
            liquidGlassTabView
        } else {
            legacyTabView
        }
    }

    @available(iOS 26, *)
    private var liquidGlassTabView: some View {
        TabView(selection: $selectedTab) {
            Tab(MainTab.home.title, systemImage: MainTab.home.selectedIcon, value: .home) {
                NavigationStack { HomeView() }
                    .toolbar(tabBarState.isVisible ? .visible : .hidden, for: .tabBar)
            }
            Tab(MainTab.aiCoach.title, systemImage: MainTab.aiCoach.selectedIcon, value: .aiCoach) {
                NavigationStack { AiCoachView() }
                    .toolbar(tabBarState.isVisible ? .visible : .hidden, for: .tabBar)
            }
            Tab(MainTab.workouts.title, systemImage: MainTab.workouts.selectedIcon, value: .workouts) {
                NavigationStack { WorkoutsView() }
                    .toolbar(tabBarState.isVisible ? .visible : .hidden, for: .tabBar)
            }
            Tab(MainTab.profile.title, systemImage: MainTab.profile.selectedIcon, value: .profile) {
                NavigationStack { ProfileView() }
                    .toolbar(tabBarState.isVisible ? .visible : .hidden, for: .tabBar)
            }
        }
        .tint(Palette.coral)
        .environmentObject(profileStore)
        .environmentObject(tabBarState)
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .onAppear { profileStore.load() }
        .onChange(of: tabBarState.pendingTabSwitch) { _, tab in
            if let tab = tab {
                selectedTab = tab
                tabBarState.pendingTabSwitch = nil
            }
        }
    }

    private var legacyTabView: some View {
        ZStack(alignment: .bottom) {

            ZStack {
                tabView(.home) { HomeView() }
                tabView(.aiCoach) { AiCoachView() }
                tabView(.workouts) { WorkoutsView() }
                tabView(.profile) { ProfileView() }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.bottom, tabBarState.isVisible ? 76 : 0)
            .environmentObject(profileStore)
            .environmentObject(tabBarState)

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
            .offset(y: tabBarState.isVisible ? 0 : 120)
            .opacity(tabBarState.isVisible ? 1 : 0)
            .allowsHitTesting(tabBarState.isVisible)
            .animation(.easeInOut(duration: 0.25), value: tabBarState.isVisible)
        }
        .background(Palette.warmOffWhite.ignoresSafeArea())
        .onAppear { profileStore.load() }
        .onChange(of: tabBarState.pendingTabSwitch) { tab in
            if let tab = tab {
                selectedTab = tab
                tabBarState.pendingTabSwitch = nil
            }
        }
    }

    @ViewBuilder
    private func tabView<Content: View>(
        _ tab: MainTab,
        @ViewBuilder content: () -> Content
    ) -> some View {
        let isActive = selectedTab == tab
        NavigationStack {
            content()
        }
        .opacity(isActive ? 1 : 0)
        .allowsHitTesting(isActive)

        .accessibilityHidden(!isActive)
    }
}

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
