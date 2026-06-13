import SwiftUI

struct PurchaseSuccessView: View {
    @EnvironmentObject var appState: AppState

    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.white.ignoresSafeArea()

            Button(action: { appState.completeOnboardingFlow() }) {
                Image(systemName: "xmark")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(darkColor)
                    .frame(width: 32, height: 32)
                    .background(Circle().fill(Color.gray.opacity(0.15)))
            }
            .padding(.leading, 20)
            .padding(.top, 8)
            .zIndex(1)

            VStack(spacing: 0) {

                Spacer().frame(height: 56)

                Spacer()

                VStack(spacing: 0) {
                    Image("ic_paywall_success")
                        .resizable()
                        .aspectRatio(1, contentMode: .fit)
                        .frame(maxWidth: .infinity)

                    Spacer().frame(height: 32)

                    Text("Успешно!\nВы разблокировали все функции")
                        .font(.system(size: 32, weight: .bold))
                        .lineSpacing(3)
                        .multilineTextAlignment(.center)
                        .foregroundColor(darkColor)

                    Spacer().frame(height: 12)

                    Text("Теперь вам доступны все возможности приложения для достижения ваших целей")
                        .font(.system(size: 21, weight: .medium))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 36)

                Spacer()

                GymGenieButton(title: "Освоить новые возможности", accentColor: Palette.coral) {
                    appState.completeOnboardingFlow()
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
    }
}
