import SwiftUI
import Shared

struct WorkoutCardSmall: View {
    let plan: WorkoutPlanShortResponse
    var onStart: () -> Void = {}

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let softCard = Color(red: 0.953, green: 0.949, blue: 0.937)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    private var isAI: Bool {
        plan.createdBy.uppercased() == "AI"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top) {
                Text(plan.name)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(deepInk)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if isAI {
                    Text("+ AI")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Capsule().fill(orange))
                }
            }

            Text("\(plan.daysCount) дней/нед.")
                .font(.system(size: 11))
                .foregroundColor(mutedText)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(Capsule().fill(softCard))

            if plan.isActive {
                HStack(spacing: 5) {
                    Circle().fill(orange).frame(width: 6, height: 6)
                    Text("АКТИВНЫЙ")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(orange)
                }
            }

            Spacer(minLength: 4)

            Button(action: onStart) {
                Text("Начать")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(RoundedRectangle(cornerRadius: 10).fill(orange))
            }
            .buttonStyle(.plain)
        }
        .padding(14)
        .frame(maxWidth: .infinity, minHeight: 150, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 16).fill(.white))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(plan.isActive ? orange : Color.clear, lineWidth: 1.5)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
    }
}
