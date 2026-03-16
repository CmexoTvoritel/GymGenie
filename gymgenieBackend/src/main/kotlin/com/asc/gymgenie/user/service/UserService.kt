package com.asc.gymgenie.user.service

import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.user.dto.UpdateProfileRequest
import com.asc.gymgenie.user.dto.UserProfileResponse
import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getProfile(userId: UUID): UserProfileResponse {
        val user = findUserById(userId)
        return user.toProfileResponse()
    }

    @Transactional
    fun updateProfile(userId: UUID, request: UpdateProfileRequest): UserProfileResponse {
        val user = findUserById(userId)

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }
        request.gender?.let { user.gender = it }
        request.birthDate?.let { user.birthDate = it }
        request.weightKg?.let { user.weightKg = it }
        request.heightCm?.let { user.heightCm = it }
        request.profilePhotoUrl?.let { user.profilePhotoUrl = it }

        return userRepository.save(user).toProfileResponse()
    }

    private fun findUserById(userId: UUID): UserEntity {
        return userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
    }

    private fun UserEntity.toProfileResponse() = UserProfileResponse(
        id = id!!,
        username = username,
        email = email,
        firstName = firstName,
        lastName = lastName,
        gender = gender,
        birthDate = birthDate,
        weightKg = weightKg,
        heightCm = heightCm,
        profilePhotoUrl = profilePhotoUrl,
        subscriptionType = subscriptionType.name
    )
}
