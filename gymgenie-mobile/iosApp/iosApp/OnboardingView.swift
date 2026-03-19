import SwiftUI

struct OnboardingView: View {
    @EnvironmentObject var appState: AppState

    @State private var currentPage: Int = 0

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)

    private let pages: [OnboardingPage] = [
        OnboardingPage(
            title: "Твой ИИ-тренер",
            subtitle: "Понимает цель и нагрузку",
            iconName: "figure.strengthtraining.traditional"
        ),
        OnboardingPage(
            title: "План в чате",
            subtitle: "Тренировки по твоим ответам",
            iconName: "message.fill"
        ),
        OnboardingPage(
            title: "Прогресс без стресса",
            subtitle: "Напоминает, поддерживает, адаптирует план",
            iconName: "chart.line.uptrend.xyaxis"
        ),
    ]

    var body: some View {
        VStack(spacing: 0) {
            TabView(selection: $currentPage) {
                ForEach(0..<pages.count, id: \.self) { index in
                    onboardingPageView(page: pages[index])
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))
            .indexViewStyle(.page(backgroundDisplayMode: .always))

            VStack(spacing: 12) {
                GymGenieButton(title: "Продолжить") {
                    advancePage()
                }

                Button(action: { skipToPrivacy() }) {
                    Text("Пропустить")
                        .font(.system(size: 14))
                        .foregroundColor(.gray)
                }
                .padding(.bottom, 8)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .background(Color(red: 0.961, green: 0.969, blue: 0.980)) // #F5F7FA
        .edgesIgnoringSafeArea(.all)
    }

    private func onboardingPageView(page: OnboardingPage) -> some View {
        VStack(spacing: 24) {
            Spacer()

            ZStack {
                RoundedRectangle(cornerRadius: 24)
                    .fill(accentColor.opacity(0.15))
                    .frame(width: 200, height: 200)

                Image(systemName: page.iconName)
                    .font(.system(size: 72))
                    .foregroundColor(accentColor)
            }

            Text(page.title)
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(Color(red: 0.1, green: 0.15, blue: 0.3))
                .multilineTextAlignment(.center)

            Text(page.subtitle)
                .font(.system(size: 16))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Spacer()
            Spacer()
        }
    }

    private func advancePage() {
        if currentPage < pages.count - 1 {
            withAnimation {
                currentPage += 1
            }
        } else {
            skipToPrivacy()
        }
    }

    private func skipToPrivacy() {
        appState.navigate(to: .privacy)
    }
}

private struct OnboardingPage {
    let title: String
    let subtitle: String
    let iconName: String
}
