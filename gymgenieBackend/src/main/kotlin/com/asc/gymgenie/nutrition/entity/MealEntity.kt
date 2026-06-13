package com.asc.gymgenie.nutrition.entity

import jakarta.persistence.*
import java.util.*

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

    var estimatedCalories: Int? = null,

    @OneToMany(mappedBy = "meal", cascade = [CascadeType.ALL], orphanRemoval = true)
    var dishes: MutableList<DishEntity> = mutableListOf()
)
