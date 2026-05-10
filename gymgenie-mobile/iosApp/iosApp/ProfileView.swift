import SwiftUI
import Shared

struct ProfileView: View {
    @EnvironmentObject private var appState: AppState
    @EnvironmentObject private var profileStore: UserProfileStoreWrapper
    @StateObject private var homeVM = HomeViewModelWrapper()

    @State private var confirmDialog: String? = nil

    private let coral = Palette.coral
    private let black = Color(red: 0.039, green: 0.039, blue: 0.039)
    private let border = Color(red: 0.929, green: 0.929, blue: 0.937)
    private let muted = Color(red: 0.545, green: 0.545, blue: 0.573)
    private let soft = Color(red: 0.957, green: 0.957, blue: 0.965)
    private let coralSoft = Color(red: 1.0, green: 0.910, blue: 0.882)
    private let dangerRed = Color(red: 0.898, green: 0.282, blue: 0.302)
    private let backgroundColor = Color(red: 0.973, green: 0.973, blue: 0.980)

    var body: some View {
        let profile = profileStore.profile
        let hasPro = profile?.subscriptionType == "PREMIUM"
        let displayName = buildDisplayName(profile)

        ZStack(alignment: .bottom) {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    headerRow(displayName: displayName)

                    VStack(spacing: 0) {
                        heroCard(profile: profile, hasPro: hasPro, displayName: displayName)
                            .padding(.top, 4)

                        if let exp = profile?.experience, !exp.isEmpty {
                            experienceStrip(
                                experience: exp,
                                frequency: profile?.frequency ?? ""
                            )
                            .padding(.top, 14)
                        }

                        subscriptionCard(hasPro: hasPro)
                            .padding(.top, 14)

                        sectionLabel("Общее")
                        settingsGroup {
                            settingsRow(label: "Язык", icon: "globe", value: "Русский")
                            Divider().padding(.leading, 56)
                            settingsRow(label: "Тема", icon: "paintpalette.fill", value: "Системная")
                            Divider().padding(.leading, 56)
                            settingsRow(label: "Уведомления", icon: "bell.fill")
                        }

                        sectionLabel("Активность")
                        settingsGroup {
                            settingsRow(label: "Статистика", icon: "chart.bar.fill")
                        }

                        sectionLabel("Поддержка")
                        settingsGroup {
                            settingsRow(label: "Помощь и FAQ", icon: "questionmark.circle.fill")
                            Divider().padding(.leading, 56)
                            settingsRow(label: "Отправить отзыв", icon: "envelope.fill")
                        }

                        sectionLabel("Аккаунт")
                        settingsGroup {
                            settingsRow(
                                label: "Выйти из аккаунта",
                                icon: "rectangle.portrait.and.arrow.right",
                                action: { confirmDialog = "logout" }
                            )
                            Divider().padding(.leading, 56)
                            settingsRow(
                                label: "Удалить аккаунт",
                                icon: "trash.fill",
                                labelColor: dangerRed,
                                iconColor: dangerRed,
                                action: { confirmDialog = "delete" }
                            )
                        }

                        Text("Версия 1.0.0")
                            .font(.system(size: 12))
                            .foregroundColor(muted)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 22)
                            .padding(.bottom, 32)
                    }
                    .padding(.horizontal, 20)
                }
            }
            .background(backgroundColor.ignoresSafeArea())
            .refreshable { await refreshAndWait() }

            if confirmDialog != nil {
                confirmSheetOverlay()
            }
        }
        .ignoresSafeArea(edges: .bottom)
        .onAppear {
            homeVM.setProfileStore(profileStore.store)
            if profileStore.profile == nil {
                homeVM.loadData()
            }
        }
        .onChange(of: homeVM.isLoggedOut) { loggedOut in
            if loggedOut { appState.navigate(to: .login) }
        }
    }

    // MARK: - Header

    private func headerRow(displayName: String) -> some View {
        HStack {
            Text("Профиль")
                .font(.system(size: 28, weight: .heavy))
                .foregroundColor(black)
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
    }

    // MARK: - Hero Card

    private func heroCard(
        profile: UserProfileResponse?,
        hasPro: Bool,
        displayName: String
    ) -> some View {
        VStack(spacing: 0) {
            profileAvatar(name: displayName, size: 76)
                .padding(.top, 22)

            Text(displayName.isEmpty ? "Пользователь" : displayName)
                .font(.system(size: 19, weight: .bold))
                .foregroundColor(black)
                .padding(.top, 12)

            if let email = profile?.email, !email.isEmpty {
                Text(email)
                    .font(.system(size: 13))
                    .foregroundColor(muted)
                    .padding(.top, 2)
            }

            if hasPro {
                HStack(spacing: 5) {
                    Image(systemName: "medal.fill")
                        .font(.system(size: 10))
                    Text("PRO MEMBER")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(0.4)
                }
                .foregroundColor(.white)
                .padding(.horizontal, 11)
                .padding(.vertical, 5)
                .background(Capsule().fill(black))
                .padding(.top, 10)
            } else {
                Text("FREE")
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.4)
                    .foregroundColor(muted)
                    .padding(.horizontal, 11)
                    .padding(.vertical, 5)
                    .background(Capsule().fill(soft))
                    .padding(.top, 10)
            }

            HStack(spacing: 0) {
                statItem(
                    value: profile?.weightKg.map { "\(Int($0.doubleValue))" } ?? "—",
                    unit: "кг",
                    label: "Вес"
                )
                Rectangle().fill(border).frame(width: 1, height: 32)
                statItem(
                    value: profile?.heightCm.map { "\(Int($0.doubleValue))" } ?? "—",
                    unit: "см",
                    label: "Рост"
                )
                Rectangle().fill(border).frame(width: 1, height: 32)
                statItem(
                    value: ageValue(profile: profile),
                    unit: ageValue(profile: profile) == "—" ? "" : "лет",
                    label: "Возраст"
                )
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .padding(.horizontal, 4)
            .background(RoundedRectangle(cornerRadius: 14).fill(backgroundColor))
            .padding(.top, 18)
            .padding(.bottom, 22)
        }
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 24)
                .fill(Color.white)
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(border, lineWidth: 1))
        )
    }

    private func profileAvatar(name: String, size: CGFloat) -> some View {
        let initials = name.trimmingCharacters(in: .whitespaces)
            .components(separatedBy: .whitespaces)
            .prefix(2)
            .compactMap { $0.first.map { String($0).uppercased() } }
            .joined()
        return ZStack {
            Circle()
                .fill(LinearGradient(
                    colors: [coral, Color(red: 1.0, green: 0.541, blue: 0.431)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ))
                .frame(width: size, height: size)
            Text(initials.isEmpty ? "?" : initials)
                .font(.system(size: size * 0.36, weight: .bold))
                .foregroundColor(.white)
        }
    }

    private func statItem(value: String, unit: String, label: String) -> some View {
        VStack(spacing: 2) {
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(value)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(black)
                if value != "—" && !unit.isEmpty {
                    Text(unit)
                        .font(.system(size: 11))
                        .foregroundColor(muted)
                }
            }
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(muted)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Experience Strip

    private func experienceStrip(experience: String, frequency: String) -> some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 12)
                .fill(coralSoft)
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: "dumbbell.fill")
                        .font(.system(size: 16))
                        .foregroundColor(coral)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text("ОПЫТ")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(muted)
                    .tracking(0.6)
                let text = frequency.isEmpty
                    ? experience
                    : "\(experience) · \(frequency.lowercased())"
                Text(text)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(black)
            }
            Spacer()
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 18)
                .fill(Color.white)
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(border, lineWidth: 1))
        )
    }

    // MARK: - Subscription Card

    @ViewBuilder
    private func subscriptionCard(hasPro: Bool) -> some View {
        if hasPro {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(coralSoft)
                    .frame(width: 42, height: 42)
                    .overlay(
                        Image(systemName: "medal.fill")
                            .font(.system(size: 18))
                            .foregroundColor(coral)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text("Premium Plan")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(black)
                    Text("Активна")
                        .font(.system(size: 13))
                        .foregroundColor(muted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13))
                    .foregroundColor(Color(red: 0.784, green: 0.784, blue: 0.808))
            }
            .padding(18)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(Color.white)
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(coral, lineWidth: 1.5))
            )
        } else {
            Button(action: { appState.navigate(to: .paywall) }) {
                HStack(spacing: 12) {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(coral.opacity(0.18))
                        .frame(width: 42, height: 42)
                        .overlay(
                            Image(systemName: "medal.fill")
                                .font(.system(size: 18))
                                .foregroundColor(coral)
                        )
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Открой Premium")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                        Text("AI-планы, статистика, без рекламы")
                            .font(.system(size: 13))
                            .foregroundColor(Color(white: 0.65))
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13))
                        .foregroundColor(.white)
                }
                .padding(18)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 20)
                        .fill(LinearGradient(
                            colors: [black, Color(red: 0.122, green: 0.122, blue: 0.133)],
                            startPoint: .leading,
                            endPoint: .trailing
                        ))
                )
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Settings

    private func sectionLabel(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.system(size: 11, weight: .semibold))
            .foregroundColor(muted)
            .tracking(0.8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading, 4)
            .padding(.top, 24)
            .padding(.bottom, 10)
    }

    private func settingsGroup<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 0) {
            content()
        }
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 18)
                .fill(Color.white)
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(border, lineWidth: 1))
        )
    }

    private func settingsRow(
        label: String,
        icon: String,
        value: String? = nil,
        labelColor: Color? = nil,
        iconColor: Color? = nil,
        action: (() -> Void)? = nil
    ) -> some View {
        let tint = iconColor ?? black
        let isDestructive = iconColor == dangerRed
        let iconBg = isDestructive
            ? Color(red: 0.996, green: 0.906, blue: 0.910)
            : soft

        return Button(action: { action?() }) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 10)
                    .fill(iconBg)
                    .frame(width: 34, height: 34)
                    .overlay(
                        Image(systemName: icon)
                            .font(.system(size: 15))
                            .foregroundColor(tint)
                    )
                Text(label)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(labelColor ?? black)
                Spacer()
                if let value {
                    Text(value)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(muted)
                }
                Image(systemName: "chevron.right")
                    .font(.system(size: 13))
                    .foregroundColor(Color(red: 0.784, green: 0.784, blue: 0.808))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 15)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Confirm Sheet

    @ViewBuilder
    private func confirmSheetOverlay() -> some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture { confirmDialog = nil }

            let isDelete = confirmDialog == "delete"

            VStack(spacing: 0) {
                Capsule()
                    .fill(Color(white: 0.85))
                    .frame(width: 42, height: 4)
                    .padding(.top, 16)

                ZStack {
                    Circle()
                        .fill(isDelete
                              ? Color(red: 0.996, green: 0.906, blue: 0.910)
                              : coralSoft)
                        .frame(width: 56, height: 56)
                    Image(systemName: isDelete
                          ? "trash.fill"
                          : "rectangle.portrait.and.arrow.right")
                        .font(.system(size: 22))
                        .foregroundColor(isDelete ? dangerRed : coral)
                }
                .padding(.top, 16)

                Text(isDelete ? "Удалить аккаунт?" : "Выйти из аккаунта?")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(black)
                    .padding(.top, 14)

                Text(isDelete
                     ? "Все ваши тренировки и прогресс будут безвозвратно удалены."
                     : "Чтобы продолжить, нужно будет войти заново.")
                    .font(.system(size: 14))
                    .foregroundColor(muted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                    .padding(.top, 8)

                Button(action: {
                    let which = confirmDialog
                    confirmDialog = nil
                    if which == "logout" {
                        homeVM.logout()
                    }
                }) {
                    Text(isDelete ? "Удалить навсегда" : "Выйти")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Capsule().fill(isDelete ? dangerRed : coral))
                }
                .padding(.horizontal, 22)
                .padding(.top, 22)

                Button(action: { confirmDialog = nil }) {
                    Text("Отмена")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(muted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .padding(.horizontal, 22)
                .padding(.bottom, max(16, 34))
            }
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(Color.white)
                    .ignoresSafeArea(edges: .bottom)
            )
        }
        .ignoresSafeArea()
    }

    // MARK: - Helpers

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
