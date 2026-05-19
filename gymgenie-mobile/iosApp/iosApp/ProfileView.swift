import SwiftUI
import Shared

struct ProfileView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var profileStore: UserProfileStoreWrapper
    @EnvironmentObject private var tabBarState: TabBarState
    @StateObject private var homeVM = HomeViewModelWrapper()

    @State private var confirmDialog: String? = nil
    @State private var showEdit: Bool = false
    @State private var showHistory: Bool = false

    private let backgroundColor = Color(red: 0.980, green: 0.976, blue: 0.969)
    private let pvBlack = Color(red: 0.039, green: 0.039, blue: 0.039)
    private let pvDangerRed = Color(red: 0.898, green: 0.282, blue: 0.302)
    private let pvVersionText = Color(red: 0.710, green: 0.710, blue: 0.741)

    var body: some View {
        let profile = profileStore.profile
        let hasPro = profile?.subscriptionType == "PREMIUM"
        let displayName = buildDisplayName(profile)

        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 0) {
                GymGenieToolbar(
                    title: "Профиль",
                    actions: [
                        ToolbarAction(
                            content: AnyView(
                                HStack(spacing: 6) {
                                    Image(systemName: "pencil")
                                        .font(.system(size: 16))
                                        .foregroundColor(pvBlack)
                                    Text("Ред.")
                                        .font(.system(size: 18, weight: .regular))
                                        .foregroundColor(pvBlack)
                                }
                            ),
                            action: { showEdit = true }
                        ),
                    ]
                )

                VStack(spacing: 0) {
                    HeroCard(
                        displayName: displayName,
                        email: profile?.email ?? "",
                        hasPro: hasPro,
                        weightKg: profile?.weightKg.map { "\(Int($0.doubleValue))" } ?? "—",
                        heightCm: profile?.heightCm.map { "\(Int($0.doubleValue))" } ?? "—",
                        ageYears: ageValue(profile: profile)
                    )
                    .padding(.top, 4)

                    if let exp = profile?.experience, !exp.isEmpty {
                        ExperienceStrip(
                            experience: exp,
                            frequency: profile?.frequency ?? ""
                        )
                        .padding(.top, 14)
                    }

                    SubscriptionCardView(
                        hasPro: hasPro,
                        onTap: { if !hasPro { appState.navigate(to: .paywall) } }
                    )
                    .padding(.top, 14)

                    ProfileSectionLabel(text: "Общее")
                    SettingsGroupView {
                        SettingsRowView(label: "Язык", icon: "globe", value: "Русский")
                        Divider().padding(.leading, 56)
                        SettingsRowView(label: "Тема", icon: "paintpalette.fill", value: "Системная")
                        Divider().padding(.leading, 56)
                        SettingsRowView(label: "Уведомления", icon: "bell.fill")
                    }

                    ProfileSectionLabel(text: "Активность")
                    SettingsGroupView {
                        SettingsRowView(label: "Статистика", icon: "chart.bar.fill", action: { showHistory = true })
                    }

                    ProfileSectionLabel(text: "Поддержка")
                    SettingsGroupView {
                        SettingsRowView(label: "Помощь и FAQ", icon: "questionmark.circle.fill")
                        Divider().padding(.leading, 56)
                        SettingsRowView(label: "Отправить отзыв", icon: "envelope.fill")
                    }

                    ProfileSectionLabel(text: "Аккаунт")
                    SettingsGroupView {
                        SettingsRowView(
                            label: "Выйти из аккаунта",
                            icon: "rectangle.portrait.and.arrow.right",
                            action: { confirmDialog = "logout" }
                        )
                        Divider().padding(.leading, 56)
                        SettingsRowView(
                            label: "Удалить аккаунт",
                            icon: "trash.fill",
                            labelColor: pvDangerRed,
                            iconColor: pvDangerRed,
                            action: { confirmDialog = "delete" }
                        )
                    }

                    Text("Версия \(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "—")")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(pvVersionText)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 22)
                        .padding(.bottom, 32)
                }
                .padding(.horizontal, 20)
            }
        }
        .background(backgroundColor.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .refreshable { await refreshAndWait() }
        .sheet(isPresented: confirmDialogBinding) {
            ConfirmAccountSheet(
                isDelete: confirmDialog == "delete",
                onConfirm: {
                    let which = confirmDialog
                    confirmDialog = nil
                    if which == "logout" {
                        homeVM.logout()
                    }
                },
                onDismiss: { confirmDialog = nil }
            )
            .presentationDetents([.height(300)])
            .presentationDragIndicator(.visible)
        }
        .navigationDestination(isPresented: $showHistory) {
            WorkoutHistoryView(onBack: { showHistory = false })
                .toolbar(.hidden, for: .navigationBar)
        }
        .navigationDestination(isPresented: $showEdit) {
            EditProfileView(onBack: { showEdit = false })
                .environmentObject(profileStore)
        }
        .onAppear {
            homeVM.setProfileStore(profileStore.store)
            if profileStore.profile == nil {
                homeVM.loadData()
            }
        }
        .onChange(of: homeVM.isLoggedOut) { loggedOut in
            if loggedOut { appState.navigate(to: .login) }
        }
        .onChange(of: showHistory) { showing in
            tabBarState.isVisible = !showing
        }
        .onChange(of: showEdit) { showing in
            tabBarState.isVisible = !showing
        }
    }

    private var confirmDialogBinding: Binding<Bool> {
        Binding(
            get: { confirmDialog != nil },
            set: { isPresented in
                if !isPresented { confirmDialog = nil }
            }
        )
    }

    private func buildDisplayName(_ profile: UserProfileResponse?) -> String {
        guard let p = profile else { return "" }
        let parts = [p.firstName, p.lastName].compactMap { $0 }.filter { !$0.isEmpty }
        if !parts.isEmpty { return parts.joined(separator: " ") }
        return p.username
    }

    private func ageValue(profile: UserProfileResponse?) -> String {
        if let age = profile?.ageYears { return "\(age.intValue)" }
        guard let birthDate = profile?.birthDate else { return "—" }
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        guard let date = fmt.date(from: birthDate) else { return "—" }
        let years = Calendar.current.dateComponents([.year], from: date, to: Date()).year ?? 0
        return "\(years)"
    }

    private func refreshAndWait() async {
        homeVM.refresh()
        let poll: UInt64 = 50_000_000
        var waited: UInt64 = 0
        while !homeVM.isRefreshing && waited < 500_000_000 {
            try? await Task.sleep(nanoseconds: poll)
            waited += poll
        }
        var elapsed: UInt64 = 0
        while homeVM.isRefreshing && elapsed < 15_000_000_000 {
            try? await Task.sleep(nanoseconds: poll)
            elapsed += poll
        }
    }
}
