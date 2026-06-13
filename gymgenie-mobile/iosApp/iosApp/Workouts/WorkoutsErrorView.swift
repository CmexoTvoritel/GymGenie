import SwiftUI

struct WorkoutsErrorView: View {
    let message: String
    var onRetry: () -> Void

    private let orange = Color(red: 0.941, green: 0.439, blue: 0.188)
    private let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 36))
                .foregroundColor(orange)
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(mutedText)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            Button(action: onRetry) {
                Text("Повторить")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(Capsule().fill(orange))
            }
            .buttonStyle(.plain)
        }
    }
}
