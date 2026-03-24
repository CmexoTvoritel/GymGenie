import SwiftUI
import Shared

struct ExerciseCard: View {
    let exercise: ExerciseShortResponse

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let darkColor = Color(red: 0.102, green: 0.102, blue: 0.180)

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Image placeholder
            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.gray.opacity(0.15))
                    .frame(height: 100)
                    .overlay(
                        Image(systemName: "figure.strengthtraining.traditional")
                            .font(.system(size: 28))
                            .foregroundColor(.gray.opacity(0.4))
                    )

                if !exercise.muscleGroup.isEmpty {
                    Text(exercise.muscleGroup)
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Capsule().fill(accentColor))
                        .padding(8)
                }
            }

            Text(exercise.nameRu)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(darkColor)
                .lineLimit(2)

            if let duration = exercise.durationMinutes {
                HStack(spacing: 4) {
                    Image(systemName: "clock")
                        .font(.system(size: 10))
                        .foregroundColor(.gray)
                    Text("\(duration) мин")
                        .font(.system(size: 11))
                        .foregroundColor(.gray)
                }
            }

            if !exercise.difficultyLevel.isEmpty {
                Text(exercise.difficultyLevel)
                    .font(.system(size: 10))
                    .foregroundColor(.gray)
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(.white)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 4, y: 2)
    }
}
