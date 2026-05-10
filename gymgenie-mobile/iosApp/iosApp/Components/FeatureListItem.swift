import SwiftUI

struct FeatureListItem: View {
    let text: String
    let isFirst: Bool
    let isLast: Bool

    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        HStack(spacing: 14) {
            VStack(spacing: 0) {
                if !isFirst {
                    Rectangle()
                        .fill(Palette.coral.opacity(0.3))
                        .frame(width: 2, height: 12)
                } else {
                    Spacer().frame(width: 2, height: 12)
                }

                Circle()
                    .fill(Palette.coral)
                    .frame(width: 12, height: 12)

                if !isLast {
                    Rectangle()
                        .fill(Palette.coral.opacity(0.3))
                        .frame(width: 2, height: 12)
                } else {
                    Spacer().frame(width: 2, height: 12)
                }
            }

            Text(text)
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(darkColor)
        }
    }
}
