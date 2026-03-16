package com.asc.gymgenie.common.security

import com.asc.gymgenie.user.repository.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.*

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email: $email")
        return User.builder()
            .username(user.id.toString())
            .password(user.passwordHash)
            .authorities("ROLE_USER")
            .build()
    }

    fun loadUserById(userId: UUID): UserDetails {
        val user = userRepository.findById(userId)
            .orElseThrow { UsernameNotFoundException("User not found with id: $userId") }
        return User.builder()
            .username(user.id.toString())
            .password(user.passwordHash)
            .authorities("ROLE_USER")
            .build()
    }
}
