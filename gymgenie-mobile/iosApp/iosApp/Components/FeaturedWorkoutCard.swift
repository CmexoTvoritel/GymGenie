import SwiftUI
import Shared

struct FeaturedWorkoutCard: View {
    let plan: WorkoutPlanShortResponse

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Today plan")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(Color.white.opacity(0.3)))

                Spacer()

                Text("09:00")
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.8))
            }

            Text(plan.name)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)

            if let description = plan.description_ {
                Text(description)
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.8))
                    .lineLimit(2)
            }

            HStack {
                Text("\(plan.daysCount) дн.")
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.7))

                Spacer()
            }

            HStack(spacing: 12) {
                Button(action: {}) {
                    Text("Детали")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().stroke(.white, lineWidth: 1.5)
                        )
                }

                Button(action: {}) {
                    Text("Начать")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(accentColor)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(
                            Capsule().fill(.white)
                        )
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [accentColor, accentColor.opacity(0.7)]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
        )
    }
}
