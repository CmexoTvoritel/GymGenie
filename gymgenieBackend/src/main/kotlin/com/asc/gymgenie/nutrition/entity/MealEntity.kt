package com.asc.gymgenie.nutrition.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "meals")
class MealEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_day_id", nullable = false)
    var mealPlanDay: MealPlanDayEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var mealType: MealType,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    var totalCalories: Int = 0,

    var totalProteinG: Double = 0.0,

    var totalFatG: Double = 0.0,

    var totalCarbsG: Double = 0.0,

    @Column(nullable = false)
    var orderIndex: Int = 0,

    @OneToMany(mappedBy = "meal", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    var items: MutableList<MealItemEntity> = mutableListOf()
)
