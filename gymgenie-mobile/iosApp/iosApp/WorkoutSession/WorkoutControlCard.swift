import SwiftUI

struct WorkoutControlCard: View {
    let label: String
    let value: Double
    let isDecimal: Bool
    var onMinus: () -> Void
    var onPlus: () -> Void

    var body: some View {
        VStack(spacing: 5) {
            Text(label)
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(.gray)

            Text(isDecimal ? (value.truncatingRemainder(dividingBy: 1) == 0 ? String(format: "%.0f", value) : String(format: "%.1f", value)) : String(Int(value)))
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(Palette.deepInk)
                .lineLimit(1)

            HStack(spacing: 12) {
                Button(action: onMinus) {
                    Image(systemName: "minus")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(Palette.coral)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(Color.white))
                }
                .buttonStyle(.plain)

                Button(action: onPlus) {
                    Image(systemName: "plus")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(Palette.coral))
                        .shadow(color: Palette.coral.opacity(0.4), radius: 4, y: 2)
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: 16).fill(.white))
        .shadow(color: .black.opacity(0.05), radius: 6, y: 2)
    }
}
