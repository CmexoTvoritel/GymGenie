import SwiftUI

struct PremiumLockedOverlay: View {
    let onUnlock: () -> Void

    var body: some View {
        ZStack {
            Palette.warmOffWhite.ignoresSafeArea()

            VStack(spacing: 16) {
                HStack(spacing: 8) {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Palette.mutedText)
                    Text("Доступно в Premium")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(Palette.deepInk)
                }
                GymGenieButton(
                    title: "Разблокировать",
                    accentColor: Palette.coral,
                    action: onUnlock
                )
                .frame(maxWidth: 240)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
