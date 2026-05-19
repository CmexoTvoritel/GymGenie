import SwiftUI

struct GymGenieTextField: View {
    let placeholder: String
    @Binding var text: String
    var icon: String = ""
    var isSecure: Bool = false
    var accentColor: Color = Color(.sRGB, red: 0.4, green: 0.4, blue: 0.4)

    @State private var isPasswordVisible: Bool = false
    @FocusState private var isFocused: Bool

    var body: some View {
        HStack(spacing: 12) {
            if !icon.isEmpty {
                Image(systemName: icon)
                    .foregroundColor(accentColor)
                    .frame(width: 20)
            }

            if isSecure && !isPasswordVisible {
                SecureField(placeholder, text: $text)
                    .font(.system(size: 16))
                    .textContentType(.password)
                    .autocorrectionDisabled(true)
                    .tint(accentColor)
                    .focused($isFocused)
            } else {
                TextField(placeholder, text: $text)
                    .font(.system(size: 16))
                    .textContentType(isSecure ? .password : .none)
                    .autocorrectionDisabled(true)
                    .tint(accentColor)
                    .focused($isFocused)
            }

            if isSecure {
                Button(action: {
                    isPasswordVisible.toggle()
                }) {
                    Image(systemName: isPasswordVisible ? "eye.slash" : "eye")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(.horizontal, 16)
        .frame(height: 52)
        .contentShape(Rectangle())
        .simultaneousGesture(TapGesture().onEnded { isFocused = true })
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.gray.opacity(0.4), lineWidth: 1)
        )
    }
}
