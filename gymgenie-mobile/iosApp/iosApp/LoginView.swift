import SwiftUI

struct LoginView: View {
    @EnvironmentObject var appState: AppState
    @ObservedObject var viewModel: AuthViewModelWrapper

    @State private var emailField: String = ""
    @State private var passwordField: String = ""

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)

    /// Resigns the first responder, dismissing the on-screen keyboard.
    /// Used for taps on empty space inside the auth card and for the
    /// register-link button — text fields keep their own tap behavior.
    private func hideKeyboard() {
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil,
            from: nil,
            for: nil
        )
    }

    var body: some View {
        ZStack(alignment: .top) {
            // Top illustration area
            accentColor.opacity(0.15)
                .frame(height: 280)
                .overlay(
                    VStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: 20)
                            .fill(accentColor.opacity(0.3))
                            .frame(width: 120, height: 120)
                            .overlay(
                                Image(systemName: "figure.run")
                                    .font(.system(size: 48))
                                    .foregroundColor(accentColor)
                            )
                    }
                    .padding(.top, 60)
                )
                .edgesIgnoringSafeArea(.top)

            // Content card
            ScrollView {
                VStack(spacing: 0) {
                    Spacer()
                        .frame(height: 240)

                    VStack(spacing: 20) {
                        // Title
                        HStack {
                            Text("С возвращением")
                                .font(.system(size: 26, weight: .bold))
                                .foregroundColor(Color(red: 0.1, green: 0.15, blue: 0.3))
                            Spacer()
                        }

                        // Error message
                        if let error = viewModel.errorMessage {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(.red)
                                Text(error)
                                    .font(.system(size: 13))
                                    .foregroundColor(.red)
                                Spacer()
                            }
                            .padding(12)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.red.opacity(0.1))
                            )
                        }

                        // Email field
                        GymGenieTextField(
                            placeholder: "Email",
                            text: $emailField,
                            icon: "envelope"
                        )
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .onChange(of: emailField) { newValue in
                            viewModel.onEmailChanged(newValue)
                        }

                        // Password field
                        GymGenieTextField(
                            placeholder: "Пароль",
                            text: $passwordField,
                            icon: "lock",
                            isSecure: true
                        )
                        .onChange(of: passwordField) { newValue in
                            viewModel.onPasswordChanged(newValue)
                        }

                        // Login button
                        GymGenieButton(
                            title: "Войти",
                            isLoading: viewModel.isLoading
                        ) {
                            viewModel.login()
                        }

                        // Divider with text
                        HStack {
                            Rectangle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(height: 1)
                            Text("Войдите через")
                                .font(.system(size: 12))
                                .foregroundColor(.gray)
                                .layoutPriority(1)
                            Rectangle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(height: 1)
                        }
                        .padding(.vertical, 4)

                        // Social login row
                        HStack(spacing: 20) {
                            SocialIconButton(label: "G")
                            SocialIconButton(systemImage: "apple.logo")
                            SocialIconButton(label: "VK")
                            SocialIconButton(systemImage: "paperplane.fill")
                        }

                        // Register link
                        HStack(spacing: 4) {
                            Text("Еще нет аккаунта?")
                                .font(.system(size: 14))
                                .foregroundColor(.gray)
                            Button(action: {
                                hideKeyboard()
                                viewModel.clearFields()
                                emailField = ""
                                passwordField = ""
                                appState.navigate(to: .register)
                            }) {
                                Text("Зарегистрируйтесь")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(accentColor)
                            }
                        }
                        .padding(.top, 8)
                    }
                    .padding(24)
                    .background(
                        RoundedRectangle(cornerRadius: 24)
                            .fill(.white)
                    )
                    .offset(y: -40)
                    // Empty areas inside the white card (paddings, divider row,
                    // social row gaps) still dismiss the keyboard. Interactive
                    // children (TextFields, Buttons) capture their own taps,
                    // so this only fires for truly empty space.
                    .contentShape(Rectangle())
                    .onTapGesture { hideKeyboard() }
                }
            }
        }
        .background(backgroundColor)
        .edgesIgnoringSafeArea(.all)
        .onChange(of: viewModel.loginSuccess) { success in
            if success {
                viewModel.consumeLoginSuccess()
                appState.completeLogin()
            }
        }
    }
}

struct SocialIconButton: View {
    var label: String? = nil
    var systemImage: String? = nil

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)

    var body: some View {
        ZStack {
            if let systemImage = systemImage {
                Image(systemName: systemImage)
                    .font(.system(size: 18))
                    .foregroundColor(accentColor)
            } else if let label = label {
                Text(label)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(accentColor)
            }
        }
        .frame(width: 48, height: 48)
        .background(
            Circle()
                .fill(accentColor.opacity(0.1))
        )
    }
}
