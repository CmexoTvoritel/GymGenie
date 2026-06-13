import SwiftUI
import Shared

struct ExerciseCard: View {
    let exercise: ExerciseShortResponse
    var onTap: () -> Void = {}
    var onLongPress: (() -> Void)? = nil
    var onInfoTap: (() -> Void)? = nil

    private let cardBorder = Color(red: 0.929, green: 0.929, blue: 0.937)
    private let imageBackground = Color(red: 0.973, green: 0.973, blue: 0.980)
    private let primaryText = Color(red: 0.039, green: 0.039, blue: 0.039)
    private let metaText = Color(red: 0.298, green: 0.298, blue: 0.325)
    private let infoBadgeColor = Color(red: 0.941, green: 0.439, blue: 0.188)

    var body: some View {

        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                imageSection

                VStack(alignment: .leading, spacing: 0) {
                    Text(exercise.nameRu)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(primaryText)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                        .lineSpacing(2)
                        .frame(maxWidth: .infinity, minHeight: 35, alignment: .topLeading)
                        .padding(.top, 10)

                    if !exercise.difficultyLevel.isEmpty {
                        difficultyChip(exercise.difficultyLevel)
                            .padding(.top, 6)
                    }

                    metaRow
                        .padding(.top, 8)
                }
                .padding(.horizontal, 10)
                .padding(.bottom, 10)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(.white)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(cardBorder, lineWidth: 1.5)
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .shadow(color: Color.black.opacity(0.03), radius: 3, y: 1)
        }
        .buttonStyle(.plain)
        .modifier(LongPressOptionalModifier(onLongPress: onLongPress))
    }

    private struct LongPressOptionalModifier: ViewModifier {
        let onLongPress: (() -> Void)?
        func body(content: Content) -> some View {
            if let onLongPress = onLongPress {
                content.onLongPressGesture(minimumDuration: 0.4, perform: onLongPress)
            } else {
                content
            }
        }
    }

    private var imageSection: some View {
        ZStack(alignment: .topTrailing) {
            imageBackground

            Image(muscleGroupExerciseImageName(exercise.muscleGroup))
                .resizable()
                .aspectRatio(1, contentMode: .fill)
                .clipped()

            if let onInfoTap = onInfoTap {
                Button(action: onInfoTap) {
                    ZStack {
                        Circle()
                            .fill(infoBadgeColor)
                            .frame(width: 28, height: 28)
                            .shadow(color: Color.black.opacity(0.15), radius: 2, y: 1)
                        Text("i")
                            .font(.system(size: 14, weight: .bold, design: .serif))
                            .italic()
                            .foregroundColor(.white)
                    }
                }
                .buttonStyle(.plain)
                .padding(.top, 8)
                .padding(.trailing, 8)
                .accessibilityLabel("Подробнее об упражнении")
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: .infinity)
    }

    private var metaRow: some View {
        HStack(spacing: 5) {
            Circle()
                .fill(muscleGroupColor(exercise.muscleGroup))
                .frame(width: 7, height: 7)
            Text(muscleGroupNameRu(exercise.muscleGroup))
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(metaText)
                .lineLimit(1)
                .truncationMode(.tail)
        }
    }

    private func difficultyChip(_ level: String) -> some View {
        let (label, color): (String, Color) = {
            switch level.uppercased() {
            case "BEGINNER":
                return ("Легко", Color(red: 0.133, green: 0.627, blue: 0.420))
            case "INTERMEDIATE":
                return ("Средне", Color(red: 0.910, green: 0.608, blue: 0.071))
            case "ADVANCED":
                return ("Сложно", Color(red: 0.820, green: 0.263, blue: 0.263))
            default:
                return (level, metaText)
            }
        }()
        return Text(label)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(color)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color.opacity(0.12))
            .clipShape(Capsule())
    }

}
