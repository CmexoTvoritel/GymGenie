package com.asc.gymgenie.presentation

import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeTokenStorage : TokenStorage {
    var savedAccessToken: String? = null
    var savedRefreshToken: String? = null

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        savedAccessToken = accessToken
        savedRefreshToken = refreshToken
    }

    override suspend fun getAccessToken(): String? = savedAccessToken
    override suspend fun getRefreshToken(): String? = savedRefreshToken

    override suspend fun clearTokens() {
        savedAccessToken = null
        savedRefreshToken = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var vm: AuthViewModel
    private lateinit var tokenStorage: FakeTokenStorage

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        tokenStorage = FakeTokenStorage()
        vm = AuthViewModel(
            authApi = AuthApi(baseUrl = "http://localhost:0"),
            tokenStorage = tokenStorage,
        )
    }

    @AfterTest
    fun tearDown() {
        vm.onCleared()
        Dispatchers.resetMain()
    }

    // --- login validation ---

    @Test
    fun login_emptyEmail_setsError() {
        vm.onPasswordChanged("password123")
        vm.login()
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun login_emptyPassword_setsError() {
        vm.onEmailChanged("test@example.com")
        vm.login()
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun login_bothEmpty_setsError() {
        vm.login()
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
    }

    @Test
    fun login_blankEmail_setsError() {
        vm.onEmailChanged("   ")
        vm.onPasswordChanged("password123")
        vm.login()
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
    }

    // --- register validation ---

    @Test
    fun register_emptyName_setsError() {
        vm.onEmailChanged("test@example.com")
        vm.onPasswordChanged("password123")
        vm.register()
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun register_emptyEmail_setsError() {
        vm.onNameChanged("John")
        vm.onPasswordChanged("password123")
        vm.register()
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
    }

    @Test
    fun register_emptyPassword_setsError() {
        vm.onNameChanged("John")
        vm.onEmailChanged("test@example.com")
        vm.register()
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
    }

    @Test
    fun register_shortPassword_setsError() {
        vm.onNameChanged("John")
        vm.onEmailChanged("test@example.com")
        vm.onPasswordChanged("12345")
        vm.register()
        assertEquals("Пароль должен быть не менее 6 символов", vm.state.value.errorMessage)
    }

    @Test
    fun register_exactSixCharPassword_passesValidation() {
        // With exactly 6 chars, validation passes (the error won't be about password length).
        // The request will fail at the network level, but password validation should not trigger.
        vm.onNameChanged("John")
        vm.onEmailChanged("test@example.com")
        vm.onPasswordChanged("123456")
        vm.register()
        // If we got past validation, errorMessage should NOT be the password-length error.
        // It could be a network error from the dummy API, or isLoading could be true.
        val error = vm.state.value.errorMessage
        assertTrue(
            error == null || error != "Пароль должен быть не менее 6 символов",
            "Expected password validation to pass for 6-char password",
        )
    }

    // --- field updates ---

    @Test
    fun onEmailChanged_updatesState() {
        vm.onEmailChanged("user@mail.com")
        assertEquals("user@mail.com", vm.state.value.email)
    }

    @Test
    fun onEmailChanged_clearsError() {
        vm.login() // sets error
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
        vm.onEmailChanged("user@mail.com")
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun onPasswordChanged_updatesState() {
        vm.onPasswordChanged("secret")
        assertEquals("secret", vm.state.value.password)
    }

    @Test
    fun onPasswordChanged_clearsError() {
        vm.login()
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
        vm.onPasswordChanged("secret")
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun onNameChanged_updatesState() {
        vm.onNameChanged("John")
        assertEquals("John", vm.state.value.name)
    }

    @Test
    fun onNameChanged_clearsError() {
        vm.login()
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
        vm.onNameChanged("John")
        assertNull(vm.state.value.errorMessage)
    }

    // --- clearError ---

    @Test
    fun clearError_clearsErrorMessage() {
        vm.login() // triggers error
        assertEquals("Заполните все поля", vm.state.value.errorMessage)
        vm.clearError()
        assertNull(vm.state.value.errorMessage)
    }

    // --- resetState ---

    @Test
    fun resetState_resetsToDefaults() {
        vm.onEmailChanged("test@example.com")
        vm.onPasswordChanged("password")
        vm.onNameChanged("John")

        vm.resetState()

        val state = vm.state.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.name)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.loginSuccess)
        assertFalse(state.registerSuccess)
        assertNull(state.accessToken)
        assertNull(state.refreshToken)
        assertEquals("FREE", state.subscriptionType)
    }

    // --- consumeLoginSuccess / consumeRegisterSuccess ---

    @Test
    fun consumeLoginSuccess_resetsFlag() {
        // We can't easily trigger loginSuccess=true without a real API call,
        // but we can verify that consuming from default state keeps it false.
        vm.consumeLoginSuccess()
        assertFalse(vm.state.value.loginSuccess)
    }

    @Test
    fun consumeRegisterSuccess_resetsFlag() {
        vm.consumeRegisterSuccess()
        assertFalse(vm.state.value.registerSuccess)
    }
}
