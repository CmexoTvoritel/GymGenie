import SwiftUI

struct ExerciseHeroView: View {
    let imageUrl: String?
    let techniqueTip: String?
    let muscleGroup: String?
    var onInfoTapped: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            if let imageUrl = imageUrl, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    placeholderView
                }
                .frame(width: 200, height: 200)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                placeholderView
            }

            Button(action: onInfoTapped) {
                Image(systemName: "info.circle")
                    .font(.system(size: 22))
                    .foregroundColor(Palette.coral)
                    .frame(width: 40, height: 40)
                    .background(Circle().fill(Palette.coral.opacity(0.12)))
            }
            .padding(10)

            if let tip = techniqueTip, !tip.isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer()
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Техника")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(.white.opacity(0.7))
                        Text(tip)
                            .font(.system(size: 11))
                            .foregroundColor(.white)
                            .lineLimit(2)
                    }
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        LinearGradient(
                            colors: [Color.clear, Color.black.opacity(0.65)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                }
                .frame(width: 200, height: 200)
                .cornerRadius(16)
                .allowsHitTesting(false)
            }
        }
        .padding(.horizontal, 16)
    }

    private var placeholderView: some View {
        RoundedRectangle(cornerRadius: 16)
            .fill(
                LinearGradient(
                    colors: [Color(red: 0.102, green: 0.102, blue: 0.180), Color(red: 0.176, green: 0.176, blue: 0.267)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .frame(width: 200, height: 200)
            .overlay(
                Image(muscleGroupExerciseImageName(muscleGroup ?? ""))
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 200, height: 200)
                    .clipped()
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
