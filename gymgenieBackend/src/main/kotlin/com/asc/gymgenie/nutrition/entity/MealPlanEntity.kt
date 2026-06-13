package com.asc.gymgenie.nutrition.entity

import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "meal_plans")
class MealPlanEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    var goal: MealGoal? = null,

    var totalCalories: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var createdBy: NutritionCreatedBy = NutritionCreatedBy.AI,

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", length = 20)
    var scheduleType: WorkoutScheduleType? = null,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "meal_plan_schedule_days",
        joinColumns = [JoinColumn(name = "meal_plan_id")]
    )
    @Column(name = "day_of_week", length = 16, nullable = false)
    var scheduleDays: MutableSet<String> = mutableSetOf(),

    @Column(name = "one_off_date")
    var oneOffDate: LocalDate? = null,

    @Column(name = "primary_meal_type", length = 16)
    var primaryMealType: String? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @OneToMany(mappedBy = "mealPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("mealType ASC")
    var meals: MutableList<MealEntity> = mutableListOf(),

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
