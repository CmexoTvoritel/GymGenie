import SwiftUI

struct MealPlanCard: View {
    let title: String
    let icon: String
    let color: Color

    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)
    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(color.opacity(0.1))
                    .frame(width: 44, height: 44)

                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(color)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(darkColor)

                Text("Нажмите для деталей")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }

            Spacer()

            Button(action: {}) {
                Text("Детали")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(accentColor)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(
                        Capsule().fill(accentColor.opacity(0.1))
                    )
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(.white)
        )
        .shadow(color: Color.black.opacity(0.03), radius: 3, y: 1)
    }
}
