import SwiftUI

struct OnboardingView: View {
    @EnvironmentObject var appState: AppState

    @State private var currentPage: Int = 0

    private let coralColor = Color(.sRGB, red: 1.0, green: 0.353, blue: 0.235)

    private let pages: [OnboardingPage] = [
        OnboardingPage(
            title: "Твой ИИ-тренер",
            subtitle: "Понимает цель и нагрузку",
            imageName: "ic_onboarding_page_1"
        ),
        OnboardingPage(
            title: "Прогресс без стресса",
            subtitle: "Напоминает, поддерживает, адаптирует план",
            imageName: "ic_onboarding_page_2"
        ),
        OnboardingPage(
            title: "План питания",
            subtitle: "Получай рекомендации по своему питанию для достижения целей",
            imageName: "ic_onboarding_page_3"
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
            .tint(coralColor)

            VStack(spacing: 12) {
                GymGenieButton(
                    title: "Продолжить",
                    accentColor: coralColor,
                    fontSize: 20,
                    fontWeight: .bold
                ) {
                    advancePage()
                }

                Button(action: { skipToPrivacy() }) {
                    Text("Пропустить")
                        .font(.system(size: 18))
                        .foregroundColor(.gray)
                }
                .padding(.bottom, 8)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 36)
        }
        .background(Color.white)
        .edgesIgnoringSafeArea(.all)
    }

    private func onboardingPageView(page: OnboardingPage) -> some View {
        VStack(spacing: 24) {
            Spacer()

            Image(page.imageName)
                .resizable()
                .aspectRatio(1, contentMode: .fit)
                .padding(.horizontal, 40)

            Text(page.title)
                .font(.system(size: 31, weight: .bold))
                .foregroundColor(Color(red: 0.1, green: 0.15, blue: 0.3))
                .multilineTextAlignment(.center)

            Text(page.subtitle)
                .font(.system(size: 21, weight: .medium))
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
    let imageName: String
}
