import SwiftUI

private let scCoralSoft = Color(red: 1.0, green: 0.910, blue: 0.882)
private let scCoral = Color(red: 1.0, green: 0.353, blue: 0.235)
private let scBlack = Color(red: 0.039, green: 0.039, blue: 0.039)
private let scMuted = Color(red: 0.545, green: 0.545, blue: 0.573)
private let scChevron = Color(red: 0.784, green: 0.784, blue: 0.808)

struct SubscriptionCardView: View {
    let hasPro: Bool
    let onTap: () -> Void

    var body: some View {
        if hasPro {
            premiumActiveCard
        } else {
            unlockPremiumCard
        }
    }

    private var premiumActiveCard: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 14)
                    .fill(scCoralSoft)
                    .frame(width: 48, height: 48)
                    .overlay(
                        Image("ic_premium_badge")
                            .resizable()
                            .renderingMode(.template)
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 24, height: 24)
                            .foregroundColor(scCoral)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text("Premium Plan")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(scBlack)
                    Text("Активна")
                        .font(.system(size: 16, weight: .regular))
                        .foregroundColor(scMuted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 24))
                    .foregroundColor(scChevron)
            }
            .padding(18)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(Color.white)
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(scCoral, lineWidth: 1.5))
            )
        }
        .buttonStyle(.plain)
    }

    private var unlockPremiumCard: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 14)
                    .fill(scCoral.opacity(0.18))
                    .frame(width: 48, height: 48)
                    .overlay(
                        Image("ic_premium_badge")
                            .resizable()
                            .renderingMode(.template)
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 24, height: 24)
                            .foregroundColor(scCoral)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text("Открой Premium")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(.white)
                    Text("AI-планы, статистика, без рекламы")
                        .font(.system(size: 16, weight: .regular))
                        .foregroundColor(Color(white: 0.65))
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 24))
                    .foregroundColor(.white)
            }
            .padding(18)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(LinearGradient(
                        colors: [scBlack, Color(red: 0.122, green: 0.122, blue: 0.133)],
                        startPoint: .leading,
                        endPoint: .trailing
                    ))
            )
        }
        .buttonStyle(.plain)
    }
}
