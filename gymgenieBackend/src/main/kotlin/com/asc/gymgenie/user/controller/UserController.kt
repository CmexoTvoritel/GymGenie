package com.asc.gymgenie.user.controller

import com.asc.gymgenie.user.dto.UpdateProfileRequest
import com.asc.gymgenie.user.dto.UserProfileResponse
import com.asc.gymgenie.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    fun getProfile(authentication: Authentication): ResponseEntity<UserProfileResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(userService.getProfile(userId))
    }

    @PutMapping("/me")
    fun updateProfile(
        authentication: Authentication,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserProfileResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(userService.updateProfile(userId, request))
    }

    @PutMapping("/me/subscription")
    fun activateSubscription(authentication: Authentication): ResponseEntity<UserProfileResponse> {
        val userId = UUID.fromString(authentication.name)
        return ResponseEntity.ok(userService.activateSubscription(userId))
    }
}
