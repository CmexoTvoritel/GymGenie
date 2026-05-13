import SwiftUI

struct WorkoutsEmptyState: View {
    var onCreate: () -> Void

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let deepInk = Color(red: 0.161, green: 0.141, blue: 0.125)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    var body: some View {
        VStack(spacing: 10) {
            Text("🏋").font(.system(size: 48))
            Text("Нет тренировочных планов")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(deepInk)
            Text("Создайте свой первый план тренировок")
                .font(.system(size: 13))
                .foregroundColor(mutedText)

            Button(action: onCreate) {
                Text("Создать первый план")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Capsule().fill(orange))
            }
            .buttonStyle(.plain)
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }
}
