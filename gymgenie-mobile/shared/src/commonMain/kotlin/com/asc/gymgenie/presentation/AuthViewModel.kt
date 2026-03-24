package com.asc.gymgenie.presentation

import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.auth.AuthException
import com.asc.gymgenie.auth.NetworkException
import com.asc.gymgenie.storage.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val registerSuccess: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

class AuthViewModel(
    private val authApi: AuthApi = AuthApi(),
    private val tokenStorage: TokenStorage,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, errorMessage = null) }
    }

    fun onUsernameChanged(username: String) {
        _state.update { it.copy(username = username, errorMessage = null) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun resetState() {
        _state.value = AuthUiState()
    }

    fun login() {
        val current = _state.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Заполните все поля") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authApi.login(
                email = current.email.trim(),
                password = current.password,
            )
            result.fold(
                onSuccess = { tokenResponse ->
                    tokenStorage.saveTokens(
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                    )
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = true,
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapAuthError(error),
                        )
                    }
                },
            )
        }
    }

    fun register() {
        val current = _state.value
        if (current.username.isBlank() || current.email.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Заполните все поля") }
            return
        }
        if (current.password.length < 6) {
            _state.update { it.copy(errorMessage = "Пароль должен быть не менее 6 символов") }
            return
        }

        scope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authApi.register(
                username = current.username.trim(),
                email = current.email.trim(),
                password = current.password,
            )
            result.fold(
                onSuccess = { tokenResponse ->
                    tokenStorage.saveTokens(
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken,
                    )
                    _state.update {
                        it.copy(
                            isLoading = false,
                            registerSuccess = true,
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapRegisterError(error),
                        )
                    }
                },
            )
        }
    }

    fun consumeLoginSuccess() {
        _state.update { it.copy(loginSuccess = false) }
    }

    fun consumeRegisterSuccess() {
        _state.update { it.copy(registerSuccess = false) }
    }

    fun onCleared() {
        scope.cancel()
    }

    private fun mapAuthError(error: Throwable): String {
        return when (error) {
            is CancellationException -> throw error
            is AuthException -> when (error.statusCode) {
                401 -> "Неверный email или пароль"
                404 -> "Пользователь не найден"
                else -> "Ошибка сервера (${error.statusCode})"
            }
            is NetworkException -> "Ошибка сети: проверьте подключение"
            else -> "Произошла неизвестная ошибка"
        }
    }

    private fun mapRegisterError(error: Throwable): String {
        return when (error) {
            is CancellationException -> throw error
            is AuthException -> when (error.statusCode) {
                409 -> "Пользователь с таким email уже существует"
                400 -> "Некорректные данные"
                else -> "Ошибка сервера (${error.statusCode})"
            }
            is NetworkException -> "Ошибка сети: проверьте подключение"
            else -> "Произошла неизвестная ошибка"
        }
    }
}
