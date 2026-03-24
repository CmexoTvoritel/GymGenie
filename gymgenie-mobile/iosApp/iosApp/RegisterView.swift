import SwiftUI

struct RegisterView: View {
    @EnvironmentObject var appState: AppState
    @ObservedObject var viewModel: AuthViewModelWrapper

    @State private var usernameField: String = ""
    @State private var emailField: String = ""
    @State private var passwordField: String = ""

    private let accentColor = Color(red: 0.173, green: 0.757, blue: 0.890)
    private let backgroundColor = Color(red: 0.961, green: 0.969, blue: 0.980)

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
                                Image(systemName: "person.badge.plus")
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
                            Text("Добро пожаловать")
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

                        // Name field
                        GymGenieTextField(
                            placeholder: "Имя",
                            text: $usernameField,
                            icon: "person"
                        )
                        .onChange(of: usernameField) { newValue in
                            viewModel.onUsernameChanged(newValue)
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

                        // Register button
                        GymGenieButton(
                            title: "Зарегистрироваться",
                            isLoading: viewModel.isLoading
                        ) {
                            viewModel.register()
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

                        // Login link
                        HStack(spacing: 4) {
                            Text("Уже есть аккаунт?")
                                .font(.system(size: 14))
                                .foregroundColor(.gray)
                            Button(action: {
                                viewModel.clearFields()
                                usernameField = ""
                                emailField = ""
                                passwordField = ""
                                appState.navigate(to: .login)
                            }) {
                                Text("Войти")
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
                }
            }
        }
        .background(backgroundColor)
        .edgesIgnoringSafeArea(.all)
        .onChange(of: viewModel.registerSuccess) { success in
            if success {
                viewModel.consumeRegisterSuccess()
                appState.completeLogin()
            }
        }
    }
}
