import SwiftUI
import Shared

struct FeaturedWorkoutCard: View {
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
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                Text(plan.name)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(deepInk)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if isAI {
                    Text("+ AI")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Capsule().fill(orange))
                }
            }

            HStack(spacing: 8) {
                tagChip(text: "\(plan.daysCount) дней/нед.")
                if let desc = plan.description_, !desc.trimmingCharacters(in: .whitespaces).isEmpty {
                    tagChip(text: String(desc.prefix(25)))
                }
            }

            if plan.isActive {
                HStack(spacing: 6) {
                    Circle().fill(orange).frame(width: 8, height: 8)
                    Text("АКТИВНЫЙ ПЛАН")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(orange)
                        .kerning(0.5)
                }
            }

            Button(action: onStart) {
                Text("Начать тренировку")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(RoundedRectangle(cornerRadius: 12).fill(orange))
            }
            .buttonStyle(.plain)
            .padding(.top, 4)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 20).fill(.white))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(plan.isActive ? orange : Color.clear, lineWidth: 2)
        )
        .shadow(color: Color.black.opacity(0.06), radius: 6, y: 2)
    }

    private func tagChip(text: String) -> some View {
        Text(text)
            .font(.system(size: 11))
            .foregroundColor(mutedText)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Capsule().fill(softCard))
    }
}
