package com.asc.gymgenie.user.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "users")
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(length = 30)
    var username: String? = null,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(length = 50)
    var firstName: String? = null,

    @Column(length = 50)
    var lastName: String? = null,

    @Enumerated(EnumType.STRING)
    var gender: Gender? = null,

    var birthDate: LocalDate? = null,

    var weightKg: Double? = null,

    var heightCm: Double? = null,

    var ageYears: Int? = null,

    @Column(length = 100)
    var experience: String? = null,

    @Column(length = 100)
    var frequency: String? = null,

    @Column(length = 2000)
    var healthIssues: String? = null,

    @Column(length = 500)
    var profilePhotoUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var authProvider: AuthProvider = AuthProvider.LOCAL,

    var providerId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var subscriptionType: SubscriptionType = SubscriptionType.FREE,

    var subscriptionExpiresAt: Instant? = null,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
