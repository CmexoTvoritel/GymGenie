import SwiftUI
import Shared

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var email: String = ""
    @Published var password: String = ""
    @Published var username: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    private let authApi: AuthApi

    init(baseUrl: String = "http://localhost:8081/api/v1") {
        self.authApi = AuthApi(baseUrl: baseUrl)
    }

    func login(onSuccess: @escaping () -> Void) {
        guard validateLoginFields() else { return }
        errorMessage = nil
        isLoading = true

        Task {
            do {
                let result = try await authApi.login(email: email, password: password)
                let token = try resultToValue(result)
                saveToken(token)
                onSuccess()
            } catch {
                errorMessage = mapError(error)
            }
            isLoading = false
        }
    }

    func register(onSuccess: @escaping () -> Void) {
        guard validateRegisterFields() else { return }
        errorMessage = nil
        isLoading = true

        Task {
            do {
                let result = try await authApi.register(username: username, email: email, password: password)
                let token = try resultToValue(result)
                saveToken(token)
                onSuccess()
            } catch {
                errorMessage = mapError(error)
            }
            isLoading = false
        }
    }

    func clearFields() {
        email = ""
        password = ""
        username = ""
        errorMessage = nil
    }

    // MARK: - Validation

    private func validateLoginFields() -> Bool {
        if email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errorMessage = "Введите email"
            return false
        }
        if password.isEmpty {
            errorMessage = "Введите пароль"
            return false
        }
        return true
    }

    private func validateRegisterFields() -> Bool {
        if username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errorMessage = "Введите имя"
            return false
        }
        if email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            errorMessage = "Введите email"
            return false
        }
        if password.count < 6 {
            errorMessage = "Пароль должен быть не менее 6 символов"
            return false
        }
        return true
    }

    // MARK: - Kotlin Result unwrapping

    private func resultToValue(_ result: Any?) throws -> TokenResponse {
        // Kotlin Result<TokenResponse> is exposed to Swift as KotlinResult (or similar).
        // The SKIE/KMP mapping depends on the toolchain version.
        // With standard KMP, suspend fun returning Result<T> exposes as:
        //   - The function itself can throw
        //   - The Result wrapper needs getOrThrow()
        //
        // We handle both patterns: direct TokenResponse or KotlinResult wrapper.
        if let token = result as? TokenResponse {
            return token
        }

        // If it comes as a Kotlin Result, try to call getOrThrow
        if let kotlinResult = result as? Shared.KotlinResult<TokenResponse> {
            if let value = kotlinResult.getOrNull() {
                return value
            }
            if let exception = kotlinResult.exceptionOrNull() {
                throw NSError(
                    domain: "AuthError",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: exception.message ?? "Неизвестная ошибка"]
                )
            }
        }

        throw NSError(
            domain: "AuthError",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Неожиданный формат ответа"]
        )
    }

    // MARK: - Error mapping

    private func mapError(_ error: Error) -> String {
        if let nsError = error as NSError? {
            let message = nsError.localizedDescription
            if message.contains("401") || message.contains("Unauthorized") {
                return "Неверный email или пароль"
            }
            if message.contains("409") || message.contains("Conflict") {
                return "Пользователь с таким email уже существует"
            }
            if message.contains("network") || message.contains("Network") || message.contains("Ошибка сети") {
                return "Ошибка сети. Проверьте подключение"
            }
        }
        return "Произошла ошибка: \(error.localizedDescription)"
    }

    // MARK: - Token persistence

    private func saveToken(_ token: TokenResponse) {
        UserDefaults.standard.set(token.accessToken, forKey: "access_token")
        UserDefaults.standard.set(token.refreshToken, forKey: "refresh_token")
    }
}
