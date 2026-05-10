package com.asc.gymgenie.nutrition.entity

import jakarta.persistence.*
import java.util.*

/**
 * A single meal within a [MealPlanEntity]. Owns one or more [DishEntity] rows
 * (each dish is a concrete food item with its own portion + macros).
 */
@Entity
@Table(name = "meals")
class MealEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "order_index", nullable = false)
    var orderIndex: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    var mealPlan: MealPlanEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var mealType: MealType,

    @Column(nullable = false, length = 100)
    var name: String,

    /**
     * Estimated calories for the whole meal. Nullable because the AI
     * may decline to estimate a value and we prefer to record `null`
     * over a fabricated number.
     */
    var estimatedCalories: Int? = null,

    @OneToMany(mappedBy = "meal", cascade = [CascadeType.ALL], orphanRemoval = true)
    var dishes: MutableList<DishEntity> = mutableListOf()
)
