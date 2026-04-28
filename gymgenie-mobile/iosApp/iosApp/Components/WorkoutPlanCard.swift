import SwiftUI
import Shared

struct WorkoutPlanCard: View {
    let plan: WorkoutPlanShortResponse
    var onStart: () -> Void = {}

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Today plan")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(accentColor))

                Spacer()

                Text("09:00")
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
            }

            Text(plan.name)
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(darkColor)

            if let description = plan.description_ {
                Text(description)
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
                    .lineLimit(2)
            }

            HStack(spacing: 12) {
                Button(action: {}) {
                    Text("Детали")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(accentColor)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().stroke(accentColor, lineWidth: 1.5)
                        )
                }

                Button(action: onStart) {
                    Text("Начать")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().fill(accentColor)
                        )
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(.white)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
    }
}
