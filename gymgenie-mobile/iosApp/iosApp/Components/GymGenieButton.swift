import SwiftUI

struct GymGenieButton: View {
    let title: String
    var isLoading: Bool = false
    var isEnabled: Bool = true
    var accentColor: Color = Color(red: 0.173, green: 0.757, blue: 0.890)
    var fontSize: CGFloat = 18
    var fontWeight: Font.Weight = .semibold
    let action: () -> Void

    var body: some View {
        Button(action: {
            if isEnabled && !isLoading {

                UIApplication.shared.sendAction(
                    #selector(UIResponder.resignFirstResponder),
                    to: nil,
                    from: nil,
                    for: nil
                )
                action()
            }
        }) {
            ZStack {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    Text(title)
                        .font(.system(size: fontSize, weight: fontWeight))
                        .foregroundColor(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(isEnabled ? accentColor : accentColor.opacity(0.4))
            )
        }
        .disabled(!isEnabled || isLoading)
    }
}
