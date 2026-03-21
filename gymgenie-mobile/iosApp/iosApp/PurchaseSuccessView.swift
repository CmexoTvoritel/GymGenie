import SwiftUI

struct PurchaseSuccessView: View {
    @EnvironmentObject var appState: AppState

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let greenColor = Color(red: 0.180, green: 0.800, blue: 0.443)

    var body: some View {
        ZStack {
            // Gradient background
            VStack(spacing: 0) {
                LinearGradient(
                    gradient: Gradient(colors: [
                        greenColor.opacity(0.3),
                        greenColor.opacity(0.1),
                        Color(red: 0.961, green: 0.969, blue: 0.980),
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .edgesIgnoringSafeArea(.all)

            VStack(spacing: 24) {
                Spacer()

                // Illustration placeholder
                ZStack {
                    Circle()
                        .fill(greenColor.opacity(0.15))
                        .frame(width: 140, height: 140)

                    Image(systemName: "figure.strengthtraining.traditional")
                        .font(.system(size: 56))
                        .foregroundColor(greenColor)
                }

                // Title
                Text("Успешно!")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.180))

                // Subtitle
                Text("Вы разблокировали все функции")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(Color(red: 0.102, green: 0.102, blue: 0.180))

                Text("Теперь давайте начнём с вашего ИИ-плана и изучим все возможности приложения")
                    .font(.system(size: 15))
                    .foregroundColor(.gray)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)

                Spacer()

                // Button
                GymGenieButton(title: "Начать с планом от ИИ") {
                    appState.completeOnboardingFlow()
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 40)
            }
        }
    }
}
