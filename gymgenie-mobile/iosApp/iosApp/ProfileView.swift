import SwiftUI
import Shared

struct ProfileView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var homeVM = HomeViewModelWrapper()
    @State private var darkMode = false
    @State private var notifications = true

    private let greenColor = Color(red: 0.133, green: 0.773, blue: 0.369)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                // Title
                Text("Профиль")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(darkColor)
                    .padding(.horizontal, 20)
                    .padding(.top, 8)

                // User card
                userCard

                // Level card
                levelCard

                // Subscription card
                subscriptionCard

                // General settings
                settingsSection

                // Support section
                supportSection

                // Sign out
                VStack(spacing: 8) {
                    Button(action: {}) {
                        HStack(spacing: 8) {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .font(.system(size: 15))
                            Text("Выйти из аккаунта")
                                .font(.system(size: 15))
                        }
                        .foregroundColor(.red.opacity(0.8))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)

                    Text("Версия 1.0.0")
                        .font(.system(size: 12))
                        .foregroundColor(.gray)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
            }
        }
        .background(backgroundColor.ignoresSafeArea())
        .onAppear {
            if homeVM.userProfile == nil {
                homeVM.loadData()
            }
        }
        .onChange(of: homeVM.isLoggedOut) { loggedOut in
            if loggedOut {
                appState.navigate(to: .login)
            }
        }
    }

    // MARK: - User Card

    private var userCard: some View {
        VStack(spacing: 16) {
            // Avatar + info
            VStack(spacing: 8) {
                Circle()
                    .fill(greenColor.opacity(0.15))
                    .frame(width: 72, height: 72)
                    .overlay(
                        Image(systemName: "person.fill")
                            .font(.system(size: 32))
                            .foregroundColor(greenColor)
                    )

                VStack(spacing: 4) {
                    let name = homeVM.userProfile.flatMap { p in
                        [p.firstName, p.lastName].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " ")
                    }
                    Text(name?.isEmpty == false ? (name ?? homeVM.username) : homeVM.username)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(darkColor)

                    if let email = homeVM.userProfile?.email {
                        Text(email)
                            .font(.system(size: 13))
                            .foregroundColor(.gray)
                    }

                    Text("PRO MEMBER")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(Capsule().fill(greenColor))
                }
            }

            Divider()

            // Stats row
            HStack(spacing: 0) {
                statsCell(
                    value: homeVM.userProfile?.weightKg.map { "\(Int($0)) кг" } ?? "—",
                    label: "Вес"
                )
                Divider().frame(height: 36)
                statsCell(
                    value: homeVM.userProfile?.heightCm.map { "\(Int($0)) см" } ?? "—",
                    label: "Рост"
                )
                Divider().frame(height: 36)
                statsCell(
                    value: ageString(from: homeVM.userProfile?.birthDate),
                    label: "Возраст"
                )
            }
        }
        .padding(20)
        .background(RoundedRectangle(cornerRadius: 20).fill(.white))
        .shadow(color: .black.opacity(0.05), radius: 8, y: 2)
        .padding(.horizontal, 20)
    }

    private func statsCell(value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(darkColor)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Level Card

    private var levelCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("ТЕКУЩИЙ УРОВЕНЬ")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(.gray)

            HStack {
                Text("Продвинутый")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(greenColor)
                Spacer()
                Text("Начало: Новичок")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color.gray.opacity(0.15))
                        .frame(height: 8)
                    RoundedRectangle(cornerRadius: 4)
                        .fill(greenColor)
                        .frame(width: geo.size.width * 0.7, height: 8)
                }
            }
            .frame(height: 8)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(greenColor.opacity(0.08))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(greenColor.opacity(0.2), lineWidth: 1)
                )
        )
        .padding(.horizontal, 20)
    }

    // MARK: - Subscription Card

    private var subscriptionCard: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text("Premium Plan")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(Color(red: 0.255, green: 0.584, blue: 0.894))
                    Text("подтверждён")
                        .font(.system(size: 12))
                        .foregroundColor(.gray)
                }
                Text("Управление подпиской и оплатой")
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 13))
                .foregroundColor(.gray)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color(red: 0.255, green: 0.584, blue: 0.894).opacity(0.4), lineWidth: 1.5)
                .background(RoundedRectangle(cornerRadius: 16).fill(.white))
        )
        .padding(.horizontal, 20)
    }

    // MARK: - Settings

    private var settingsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("ОБЩЕЕ")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(.gray)
                .padding(.horizontal, 20)

            VStack(spacing: 0) {
                settingsRow {
                    HStack {
                        Image(systemName: "textformat.size")
                            .foregroundColor(.blue)
                            .frame(width: 24)
                        Text("Язык")
                            .font(.system(size: 15))
                            .foregroundColor(darkColor)
                        Spacer()
                        Text("Русский")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                    }
                }

                Divider().padding(.leading, 20)

                settingsRow {
                    HStack {
                        Image(systemName: "moon.stars.fill")
                            .foregroundColor(.indigo)
                            .frame(width: 24)
                        Text("Тёмная тема")
                            .font(.system(size: 15))
                            .foregroundColor(darkColor)
                        Spacer()
                        Toggle("", isOn: $darkMode)
                            .tint(greenColor)
                    }
                }

                Divider().padding(.leading, 20)

                settingsRow {
                    HStack {
                        Image(systemName: "bell.fill")
                            .foregroundColor(.orange)
                            .frame(width: 24)
                        Text("Уведомления")
                            .font(.system(size: 15))
                            .foregroundColor(darkColor)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                    }
                }
            }
            .background(RoundedRectangle(cornerRadius: 16).fill(.white))
            .shadow(color: .black.opacity(0.04), radius: 6, y: 2)
            .padding(.horizontal, 20)
        }
    }

    private var supportSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("ПОДДЕРЖКА")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(.gray)
                .padding(.horizontal, 20)

            VStack(spacing: 0) {
                settingsRow {
                    HStack {
                        Image(systemName: "questionmark.circle.fill")
                            .foregroundColor(.blue)
                            .frame(width: 24)
                        Text("Помощь / FAQ")
                            .font(.system(size: 15))
                            .foregroundColor(darkColor)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                    }
                }

                Divider().padding(.leading, 20)

                settingsRow {
                    HStack {
                        Image(systemName: "envelope.fill")
                            .foregroundColor(.green)
                            .frame(width: 24)
                        Text("Отправить отзыв")
                            .font(.system(size: 15))
                            .foregroundColor(darkColor)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12))
                            .foregroundColor(.gray)
                    }
                }
            }
            .background(RoundedRectangle(cornerRadius: 16).fill(.white))
            .shadow(color: .black.opacity(0.04), radius: 6, y: 2)
            .padding(.horizontal, 20)
        }
    }

    private func settingsRow<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        content()
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
    }

    // MARK: - Helpers

    private func ageString(from birthDate: String?) -> String {
        guard let birthDate else { return "—" }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        guard let date = formatter.date(from: birthDate) else { return "—" }
        let age = Calendar.current.dateComponents([.year], from: date, to: Date()).year ?? 0
        return "\(age) лет"
    }
}
