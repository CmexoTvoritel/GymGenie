import SwiftUI

struct PlanCard: View {
    let isSelected: Bool
    var badge: String? = nil
    let title: String
    let price: String
    var originalPrice: String? = nil
    var subtitle: String? = nil
    let onTap: () -> Void

    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    if let badge = badge {
                        Text(badge)
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(
                                Capsule().fill(Palette.coral)
                            )
                    }

                    Text(title)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(darkColor)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    HStack(spacing: 6) {
                        if let originalPrice = originalPrice {
                            Text(originalPrice)
                                .font(.system(size: 14))
                                .foregroundColor(.gray)
                                .strikethrough()
                        }
                        Text(price)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(darkColor)
                    }
                    if let subtitle = subtitle {
                        Text(subtitle)
                            .font(.system(size: 13))
                            .foregroundColor(.gray)
                    }
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(.white)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(isSelected ? Palette.coral : Color.clear, lineWidth: 2)
                    )
            )
            .shadow(color: Color.black.opacity(0.05), radius: 4, y: 2)
        }
    }
}
