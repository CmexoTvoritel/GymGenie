import SwiftUI

struct GymGenieButton: View {
    let title: String
    var isLoading: Bool = false
    var isEnabled: Bool = true
    let action: () -> Void

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890) // #2CC1E3

    var body: some View {
        Button(action: {
            if isEnabled && !isLoading {
                action()
            }
        }) {
            ZStack {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(isEnabled ? accentColor : accentColor.opacity(0.4))
            )
        }
        .disabled(!isEnabled || isLoading)
    }
}
