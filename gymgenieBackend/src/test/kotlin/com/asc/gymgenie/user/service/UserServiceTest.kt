package com.asc.gymgenie.user.service

import com.asc.gymgenie.common.exception.NotFoundException
import com.asc.gymgenie.user.dto.UpdateProfileRequest
import com.asc.gymgenie.user.entity.Gender
import com.asc.gymgenie.user.entity.SubscriptionType
import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

open class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userService = UserService(userRepository)
    }

    // ---- helpers ----

    private fun createUser(
        id: UUID = UUID.randomUUID(),
        email: String = "ivan@example.com",
        firstName: String? = "Иван",
        lastName: String? = "Петров",
        gender: Gender? = Gender.MALE,
        birthDate: LocalDate? = LocalDate.of(1995, 3, 15),
        weightKg: Double? = 82.0,
        heightCm: Double? = 178.0,
        ageYears: Int? = 29,
        experience: String? = "intermediate",
        frequency: String? = "3 раза в неделю",
        healthIssues: String? = null,
        profilePhotoUrl: String? = null,
        subscriptionType: SubscriptionType = SubscriptionType.FREE
    ): UserEntity {
        val user = UserEntity(
            email = email,
            passwordHash = "hashed",
            firstName = firstName,
            lastName = lastName,
            gender = gender,
            birthDate = birthDate,
            weightKg = weightKg,
            heightCm = heightCm,
            ageYears = ageYears,
            experience = experience,
            frequency = frequency,
            healthIssues = healthIssues,
            profilePhotoUrl = profilePhotoUrl,
            subscriptionType = subscriptionType
        )
        user.id = id
        return user
    }

    // ---- getProfile ----

    @Test
    fun getProfile_success() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId)

        every { userRepository.findById(userId) } returns Optional.of(user)

        val response = userService.getProfile(userId)

        assertEquals(userId, response.id)
        assertEquals("ivan@example.com", response.email)
        assertEquals("Иван", response.firstName)
        assertEquals("Петров", response.lastName)
        assertEquals(Gender.MALE, response.gender)
        assertEquals(LocalDate.of(1995, 3, 15), response.birthDate)
        assertEquals(82.0, response.weightKg)
        assertEquals(178.0, response.heightCm)
        assertEquals(29, response.ageYears)
        assertEquals("intermediate", response.experience)
        assertEquals("3 раза в неделю", response.frequency)
        assertEquals(SubscriptionType.FREE.name, response.subscriptionType)
    }

    @Test
    fun getProfile_notFound_throwsNotFoundException() {
        val userId = UUID.randomUUID()

        every { userRepository.findById(userId) } returns Optional.empty()

        val exception = assertFailsWith<NotFoundException> {
            userService.getProfile(userId)
        }

        assertEquals("User not found", exception.message)
    }

    // ---- updateProfile ----

    @Test
    fun updateProfile_success() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId, firstName = "Иван", weightKg = 82.0)

        val request = UpdateProfileRequest(
            firstName = "Артём",
            weightKg = 85.0,
            heightCm = 180.0,
            experience = "advanced",
            frequency = "5 раз в неделю",
            healthIssues = "Проблемы с коленями"
        )

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any<UserEntity>()) } answers { firstArg() }

        val response = userService.updateProfile(userId, request)

        assertEquals("Артём", response.firstName)
        assertEquals(85.0, response.weightKg)
        assertEquals(180.0, response.heightCm)
        assertEquals("advanced", response.experience)
        assertEquals("5 раз в неделю", response.frequency)
        assertEquals("Проблемы с коленями", response.healthIssues)
        // Fields not in request should remain unchanged
        assertEquals("Петров", response.lastName)
        assertEquals(Gender.MALE, response.gender)

        verify { userRepository.save(any<UserEntity>()) }
    }

    // ---- activateSubscription ----

    @Test
    fun activateSubscription_success() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId, subscriptionType = SubscriptionType.FREE)

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { userRepository.save(any<UserEntity>()) } answers { firstArg() }

        val response = userService.activateSubscription(userId)

        assertEquals(SubscriptionType.PREMIUM.name, response.subscriptionType)

        verify { userRepository.save(any<UserEntity>()) }
    }
}
