import SwiftUI

struct GymGenieTextField: View {
    let placeholder: String
    @Binding var text: String
    var icon: String = ""
    var isSecure: Bool = false
    var accentColor: Color = Color(.sRGB, red: 0.4, green: 0.4, blue: 0.4)

    @State private var isPasswordVisible: Bool = false

    var body: some View {
        HStack(spacing: 12) {
            if !icon.isEmpty {
                Image(systemName: icon)
                    .foregroundColor(accentColor)
                    .frame(width: 20)
            }

            if isSecure && !isPasswordVisible {
                SecureField(placeholder, text: $text)
                    .textContentType(.password)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                    .tint(accentColor)
            } else {
                TextField(placeholder, text: $text)
                    .autocapitalization(isSecure ? .none : .none)
                    .disableAutocorrection(true)
                    .textContentType(isSecure ? .password : .none)
                    .tint(accentColor)
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
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color(.systemGray4), lineWidth: 1)
        )
    }
}
