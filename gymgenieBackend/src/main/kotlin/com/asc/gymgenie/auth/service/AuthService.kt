package com.asc.gymgenie.auth.service

import com.asc.gymgenie.auth.dto.*
import com.asc.gymgenie.auth.entity.RefreshTokenEntity
import com.asc.gymgenie.auth.repository.RefreshTokenRepository
import com.asc.gymgenie.common.config.JwtProperties
import com.asc.gymgenie.common.exception.ConflictException
import com.asc.gymgenie.common.exception.UnauthorizedException
import com.asc.gymgenie.common.security.JwtProvider
import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    private val passwordEncoder: PasswordEncoder
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun register(request: RegisterRequest): TokenResponse {
        log.info("Registration attempt for email={}, firstName={}", request.email, request.firstName)

        if (userRepository.existsByEmail(request.email)) {
            log.info("Registration rejected: email '{}' already registered", request.email)
            throw ConflictException("Email already registered")
        }

        val user = userRepository.save(
            UserEntity(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password)!!,
                firstName = request.firstName,
            )
        )

        log.info("User registered successfully: id={}, email={}", user.id, user.email)
        return generateTokenResponse(user)
    }

    @Transactional
    fun login(request: LoginRequest): TokenResponse {
        log.info("Login attempt for email={}", request.email)

        val user = userRepository.findByEmail(request.email)
            ?: run {
                log.info("Login failed: no user found for email={}", request.email)
                throw UnauthorizedException("Invalid email or password")
            }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            log.info("Login failed: incorrect password for email={}", request.email)
            throw UnauthorizedException("Invalid email or password")
        }

        log.info("Login successful for user id={}", user.id)
        return generateTokenResponse(user)
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): TokenResponse {
        val refreshToken = refreshTokenRepository.findByToken(request.refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")

        if (refreshToken.expiresAt.isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken)
            throw UnauthorizedException("Refresh token expired")
        }

        val user = refreshToken.user
        refreshTokenRepository.delete(refreshToken)

        return generateTokenResponse(user)
    }

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenRepository.deleteByToken(refreshToken)
    }

    @Transactional
    fun logoutAll(userId: UUID) {
        refreshTokenRepository.deleteAllByUserId(userId)
    }

    private fun generateTokenResponse(user: UserEntity): TokenResponse {
        val accessToken = jwtProvider.generateAccessToken(user.id!!, user.email)
        val refreshToken = jwtProvider.generateRefreshToken()

        refreshTokenRepository.save(
            RefreshTokenEntity(
                user = user,
                token = refreshToken,
                expiresAt = Instant.now().plus(jwtProperties.refreshTokenExpiration)
            )
        )

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = AuthUserResponse(
                id = user.id!!,
                firstName = user.firstName ?: "",
                email = user.email,
                subscriptionType = user.subscriptionType.name,
            )
        )
    }
}
