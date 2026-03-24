import SwiftUI
import Shared

struct WorkoutCardSmall: View {
    let plan: WorkoutPlanShortResponse

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("План")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(accentColor)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Capsule().fill(accentColor.opacity(0.1)))

                Spacer()
            }

            Text(plan.name)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(darkColor)
                .lineLimit(2)

            Text("\(plan.daysCount) дн.")
                .font(.system(size: 12))
                .foregroundColor(.gray)

            Spacer(minLength: 4)

            HStack(spacing: 8) {
                Button(action: {}) {
                    Text("Детали")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(accentColor)
                }

                Spacer()

                Button(action: {}) {
                    Text("Начать")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                        .background(Capsule().fill(accentColor))
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, minHeight: 140, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(.white)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
    }
}
