import SwiftUI

struct GymGenieTextField: View {
    let placeholder: String
    @Binding var text: String
    var icon: String = ""
    var isSecure: Bool = false

    @State private var isPasswordVisible: Bool = false

    var body: some View {
        HStack(spacing: 12) {
            if !icon.isEmpty {
                Image(systemName: icon)
                    .foregroundColor(.gray)
                    .frame(width: 20)
            }

            if isSecure && !isPasswordVisible {
                SecureField(placeholder, text: $text)
                    .textContentType(.password)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
            } else {
                TextField(placeholder, text: $text)
                    .autocapitalization(isSecure ? .none : .none)
                    .disableAutocorrection(true)
                    .textContentType(isSecure ? .password : .none)
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
                .fill(Color(.systemGray6))
        )
    }
}
