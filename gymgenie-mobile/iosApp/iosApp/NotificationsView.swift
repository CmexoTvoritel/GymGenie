import SwiftUI

struct NotificationsView: View {
    var onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            GymGenieToolbar(
                title: "Уведомления",
                showBackNavigation: true,
                onBackTap: onClose
            )

            ZStack {
                Palette.warmOffWhite.ignoresSafeArea()

                VStack(spacing: 12) {
                    Spacer()

                    Text("В разработке")
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundColor(Palette.deepInk)

                    Text("Скоро здесь появится экран уведомлений")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundColor(Palette.mutedText)
                        .multilineTextAlignment(.center)

                    Spacer()
                }
                .padding(.horizontal, 24)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
    }
}
