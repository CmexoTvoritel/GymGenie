package com.asc.gymgenie.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.auth.AuthException
import com.asc.gymgenie.auth.NetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

class AuthViewModel : ViewModel() {

    private val authApi = AuthApi(baseUrl = "http://10.0.2.2:8081/api/v1")

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Заполните все поля") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authApi.login(
                email = state.email.trim(),
                password = state.password,
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { error ->
                    val message = when (error) {
                        is AuthException -> when (error.statusCode) {
                            401 -> "Неверный email или пароль"
                            404 -> "Пользователь не найден"
                            else -> "Ошибка сервера (${error.statusCode})"
                        }
                        is NetworkException -> "Ошибка сети: проверьте подключение"
                        else -> "Произошла неизвестная ошибка"
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                },
            )
        }
    }

    fun register() {
        val state = _uiState.value
        if (state.username.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Заполните все поля") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authApi.register(
                username = state.username.trim(),
                email = state.email.trim(),
                password = state.password,
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { error ->
                    val message = when (error) {
                        is AuthException -> when (error.statusCode) {
                            409 -> "Пользователь с таким email уже существует"
                            400 -> "Некорректные данные"
                            else -> "Ошибка сервера (${error.statusCode})"
                        }
                        is NetworkException -> "Ошибка сети: проверьте подключение"
                        else -> "Произошла неизвестная ошибка"
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                },
            )
        }
    }
}
