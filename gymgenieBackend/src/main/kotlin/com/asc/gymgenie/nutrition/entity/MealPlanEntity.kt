package com.asc.gymgenie.nutrition.entity

import com.asc.gymgenie.user.entity.UserEntity
import com.asc.gymgenie.workout.entity.WorkoutScheduleType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Meal plan owned by a single user.
 *
 * The plan structure is intentionally flat (plan -> meals -> dishes). It serves
 * two creation flows:
 *  - AI-generated plans, where scheduling fields ([scheduleType], [scheduleDays],
 *    [oneOffDate]) are left null/empty because the AI flow does not commit to
 *    a calendar slot for the plan.
 *  - Manually-created plans, where the user chooses a meal type and either
 *    binds the plan to specific weekdays (RECURRING) or to a one-off date.
 *
 * Scheduling is modelled as columns on the plan rather than as a separate
 * day table because each manual plan owns exactly one meal slot — there is no
 * day-level fan-out analogous to [com.asc.gymgenie.workout.entity.WorkoutPlanDayEntity].
 */
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

    /**
     * High-level user goal the plan was built for. Nullable to allow
     * plans created before the goal field was a hard requirement.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    var goal: MealGoal? = null,

    /** Estimated daily calorie target for the plan as a whole. */
    var totalCalories: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var createdBy: NutritionCreatedBy = NutritionCreatedBy.AI,

    /**
     * Scheduling mode for the plan. Nullable because legacy AI-generated rows
     * never set it and we do not want a destructive backfill. Required for
     * manual plans (validated at the service layer).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", length = 20)
    var scheduleType: WorkoutScheduleType? = null,

    /**
     * Recurring weekdays this plan is bound to (e.g. {"MONDAY", "WEDNESDAY"}).
     * Stored as upper-case strings to match how mobile clients send them in the
     * request DTO; parsed against [java.time.DayOfWeek] at the service boundary.
     * Empty for [WorkoutScheduleType.ONE_TIME] and for AI plans.
     *
     * Modeled as a [Set] (rather than a [List]) so the underlying element
     * collection table uses a composite `(meal_plan_id, day_of_week)` primary key
     * and does not require a separate ordering column. Weekday bindings are
     * inherently unordered/unique, so this is also the correct domain semantics.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "meal_plan_schedule_days",
        joinColumns = [JoinColumn(name = "meal_plan_id")]
    )
    @Column(name = "day_of_week", length = 16, nullable = false)
    var scheduleDays: MutableSet<String> = mutableSetOf(),

    /**
     * Calendar date this plan is bound to when [scheduleType] is
     * [WorkoutScheduleType.ONE_TIME]. Null for RECURRING and AI plans.
     */
    @Column(name = "one_off_date")
    var oneOffDate: LocalDate? = null,

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
