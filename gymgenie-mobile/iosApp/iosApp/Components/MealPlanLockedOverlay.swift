import SwiftUI

struct MealPlanLockedOverlay: View {
    let onUnlock: () -> Void

    private let shimmerLight = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let shimmerDark = Color(red: 0.918, green: 0.914, blue: 0.902)
    private let cardBorder = Color(red: 0.929, green: 0.929, blue: 0.937)

    var body: some View {
        ZStack {
            VStack(spacing: 8) {
                lockedSkeletonCard()
                lockedSkeletonCard()
                lockedSkeletonCard()
            }
            .opacity(0.75)
            .allowsHitTesting(false)

            Rectangle()
                .fill(Palette.warmOffWhite.opacity(0.85))
                .allowsHitTesting(false)

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
    }

    private func lockedSkeletonCard() -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 12)
                .fill(shimmerLight)
                .frame(width: 42, height: 42)

            VStack(alignment: .leading, spacing: 4) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(shimmerLight)
                    .frame(width: 80, height: 16)
                RoundedRectangle(cornerRadius: 4)
                    .fill(shimmerDark)
                    .frame(height: 14)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(alignment: .trailing, spacing: 2) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(shimmerLight)
                    .frame(width: 36, height: 16)
                RoundedRectangle(cornerRadius: 4)
                    .fill(shimmerDark)
                    .frame(width: 32, height: 12)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(RoundedRectangle(cornerRadius: 18).fill(.white))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(cardBorder, lineWidth: 1.5)
        )
    }
}
