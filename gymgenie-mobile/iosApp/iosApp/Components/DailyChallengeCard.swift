import SwiftUI

struct DailyChallengeCard: View {
    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 20)
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            accentColor,
                            accentColor.opacity(0.7),
                        ]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            HStack {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Ежедневный челлендж")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)

                    Text("Выполните задание дня и получите бонус")
                        .font(.system(size: 13))
                        .foregroundColor(.white.opacity(0.85))

                    Button(action: {}) {
                        HStack(spacing: 4) {
                            Text("Начать")
                                .font(.system(size: 14, weight: .semibold))
                            Image(systemName: "arrow.right")
                                .font(.system(size: 12, weight: .semibold))
                        }
                        .foregroundColor(accentColor)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().fill(.white)
                        )
                    }
                    .padding(.top, 4)
                }

                Spacer()

                Image(systemName: "flame.fill")
                    .font(.system(size: 48))
                    .foregroundColor(.white.opacity(0.3))
            }
            .padding(20)
        }
        .frame(height: 160)
    }
}
