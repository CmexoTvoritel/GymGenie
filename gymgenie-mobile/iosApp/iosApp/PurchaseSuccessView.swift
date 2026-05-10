import SwiftUI

struct PurchaseSuccessView: View {
    @EnvironmentObject var appState: AppState

    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.white.ignoresSafeArea()

            // Close button — absolutely positioned, does not affect layout flow.
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

            // Main column — image+texts block is vertically centered by two
            // flexible spacers so it sits between the X button area and the
            // action buttons. Buttons are part of the same VStack to avoid
            // overlapping bottom overlays.
            VStack(spacing: 0) {
                // Reserved space for the absolutely-placed X button.
                Spacer().frame(height: 56)

                Spacer()

                // Centered content block — image + both texts as one group.
                VStack(spacing: 0) {
                    Image("ic_paywall_success")
                        .resizable()
                        .aspectRatio(1, contentMode: .fit)
                        .frame(maxWidth: .infinity)

                    Spacer().frame(height: 32)

                    // Single Text with explicit line spacing — approximates
                    // 110% lineHeight at 32pt (35 - 32 = 3pt extra leading).
                    Text("Успешно!\nВы разблокировали все функции")
                        .font(.system(size: 32, weight: .bold))
                        .lineSpacing(3)
                        .multilineTextAlignment(.center)
                        .foregroundColor(darkColor)

                    Spacer().frame(height: 12)

                    Text("Теперь давайте начнём с вашего ИИ-плана и изучим все возможности приложения")
                        .font(.system(size: 21, weight: .medium))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 36)

                Spacer()

                // Bottom buttons inside the same VStack so the symmetric
                // spacers above distribute free space around the content block.
                VStack(spacing: 12) {
                    GymGenieButton(title: "Начать с планом от ИИ", accentColor: Palette.coral) {
                        appState.completeOnboardingFlow()
                    }

                    Button(action: { appState.completeOnboardingFlow() }) {
                        Text("Продолжить без плана")
                            .font(.system(size: 16))
                            .foregroundColor(.gray)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
    }
}
