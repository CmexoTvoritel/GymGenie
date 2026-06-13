package com.asc.gymgenie.auth.service

import com.asc.gymgenie.auth.dto.LoginRequest
import com.asc.gymgenie.auth.dto.RefreshTokenRequest
import com.asc.gymgenie.auth.dto.RegisterRequest
import com.asc.gymgenie.auth.entity.RefreshTokenEntity
import com.asc.gymgenie.auth.repository.RefreshTokenRepository
import com.asc.gymgenie.common.config.JwtProperties
import com.asc.gymgenie.common.exception.ConflictException
import com.asc.gymgenie.common.exception.UnauthorizedException
import com.asc.gymgenie.common.security.JwtProvider
import com.asc.gymgenie.user.entity.SubscriptionType
import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

open class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtProvider: JwtProvider
    private lateinit var jwtProperties: JwtProperties
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        refreshTokenRepository = mockk(relaxed = true)
        jwtProvider = mockk()
        jwtProperties = JwtProperties(
            secret = "test-secret-key-that-is-long-enough-for-hmac-sha256",
            accessTokenExpiration = Duration.ofMinutes(15),
            refreshTokenExpiration = Duration.ofDays(30)
        )
        passwordEncoder = mockk()

        authService = AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            jwtProvider = jwtProvider,
            jwtProperties = jwtProperties,
            passwordEncoder = passwordEncoder
        )
    }

    // ---- helpers ----

    private fun createUser(
        id: UUID = UUID.randomUUID(),
        email: String = "ivan@example.com",
        passwordHash: String = "hashed-password",
        firstName: String = "Иван"
    ): UserEntity {
        val user = UserEntity(
            email = email,
            passwordHash = passwordHash,
            firstName = firstName
        )
        user.id = id
        return user
    }

    private fun stubTokenGeneration(userId: UUID, email: String) {
        every { jwtProvider.generateAccessToken(userId, email) } returns "access-token-stub"
        every { jwtProvider.generateRefreshToken() } returns "refresh-token-stub"
        every { refreshTokenRepository.save(any()) } answers { firstArg() }
    }

    // ---- register ----

    @Test
    fun register_success() {
        val request = RegisterRequest(
            firstName = "Иван",
            email = "ivan@example.com",
            password = "securePass1"
        )

        val savedUser = createUser()

        every { userRepository.existsByEmail(request.email) } returns false
        every { passwordEncoder.encode(request.password) } returns "hashed-password"

        val userSlot = slot<UserEntity>()
        every { userRepository.save(capture(userSlot)) } answers {
            userSlot.captured.also { it.id = savedUser.id }
        }

        stubTokenGeneration(savedUser.id!!, savedUser.email)

        val response = authService.register(request)

        assertEquals("access-token-stub", response.accessToken)
        assertEquals("refresh-token-stub", response.refreshToken)
        assertEquals(savedUser.id, response.user.id)
        assertEquals("Иван", response.user.firstName)
        assertEquals("ivan@example.com", response.user.email)
        assertEquals(SubscriptionType.FREE.name, response.user.subscriptionType)

        verify { userRepository.save(any()) }
        verify { refreshTokenRepository.save(any()) }
    }

    @Test
    fun register_emailAlreadyExists_throwsConflict() {
        val request = RegisterRequest(
            firstName = "Иван",
            email = "ivan@example.com",
            password = "securePass1"
        )

        every { userRepository.existsByEmail(request.email) } returns true

        val exception = assertFailsWith<ConflictException> {
            authService.register(request)
        }

        assertEquals("Email already registered", exception.message)
    }

    // ---- login ----

    @Test
    fun login_success() {
        val request = LoginRequest(email = "ivan@example.com", password = "securePass1")
        val user = createUser()

        every { userRepository.findByEmail(request.email) } returns user
        every { passwordEncoder.matches(request.password, user.passwordHash) } returns true
        stubTokenGeneration(user.id!!, user.email)

        val response = authService.login(request)

        assertEquals("access-token-stub", response.accessToken)
        assertEquals("refresh-token-stub", response.refreshToken)
        assertEquals(user.id, response.user.id)
    }

    @Test
    fun login_wrongEmail_throwsUnauthorized() {
        val request = LoginRequest(email = "unknown@example.com", password = "securePass1")

        every { userRepository.findByEmail(request.email) } returns null

        val exception = assertFailsWith<UnauthorizedException> {
            authService.login(request)
        }

        assertEquals("Invalid email or password", exception.message)
    }

    @Test
    fun login_wrongPassword_throwsUnauthorized() {
        val request = LoginRequest(email = "ivan@example.com", password = "wrongPassword")
        val user = createUser()

        every { userRepository.findByEmail(request.email) } returns user
        every { passwordEncoder.matches(request.password, user.passwordHash) } returns false

        val exception = assertFailsWith<UnauthorizedException> {
            authService.login(request)
        }

        assertEquals("Invalid email or password", exception.message)
    }

    // ---- refresh ----

    @Test
    fun refresh_success() {
        val request = RefreshTokenRequest(refreshToken = "valid-refresh-token")
        val user = createUser()
        val refreshTokenEntity = RefreshTokenEntity(
            user = user,
            token = "valid-refresh-token",
            expiresAt = Instant.now().plusSeconds(3600)
        )
        refreshTokenEntity.id = UUID.randomUUID()

        every { refreshTokenRepository.findByToken(request.refreshToken) } returns refreshTokenEntity
        every { refreshTokenRepository.delete(refreshTokenEntity) } returns Unit
        stubTokenGeneration(user.id!!, user.email)

        val response = authService.refresh(request)

        assertEquals("access-token-stub", response.accessToken)
        assertEquals("refresh-token-stub", response.refreshToken)
        assertEquals(user.id, response.user.id)

        verify { refreshTokenRepository.delete(refreshTokenEntity) }
    }

    @Test
    fun refresh_invalidToken_throwsUnauthorized() {
        val request = RefreshTokenRequest(refreshToken = "nonexistent-token")

        every { refreshTokenRepository.findByToken(request.refreshToken) } returns null

        val exception = assertFailsWith<UnauthorizedException> {
            authService.refresh(request)
        }

        assertEquals("Invalid refresh token", exception.message)
    }

    @Test
    fun refresh_expiredToken_throwsUnauthorized() {
        val request = RefreshTokenRequest(refreshToken = "expired-token")
        val user = createUser()
        val expiredToken = RefreshTokenEntity(
            user = user,
            token = "expired-token",
            expiresAt = Instant.now().minusSeconds(3600)
        )
        expiredToken.id = UUID.randomUUID()

        every { refreshTokenRepository.findByToken(request.refreshToken) } returns expiredToken
        every { refreshTokenRepository.delete(expiredToken) } returns Unit

        val exception = assertFailsWith<UnauthorizedException> {
            authService.refresh(request)
        }

        assertEquals("Refresh token expired", exception.message)
        verify { refreshTokenRepository.delete(expiredToken) }
    }

    // ---- logout ----

    @Test
    fun logout_deletesToken() {
        val token = "some-refresh-token"

        every { refreshTokenRepository.deleteByToken(token) } returns Unit

        authService.logout(token)

        verify { refreshTokenRepository.deleteByToken(token) }
    }

    @Test
    fun logoutAll_deletesAllTokens() {
        val userId = UUID.randomUUID()

        every { refreshTokenRepository.deleteAllByUserId(userId) } returns Unit

        authService.logoutAll(userId)

        verify { refreshTokenRepository.deleteAllByUserId(userId) }
    }
}
