package com.asc.gymgenie.nutrition.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "dishes")
class DishEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    var meal: MealEntity,

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Column(length = 50)
    var portionDescription: String? = null,

    var calories: Int? = null,

    var proteinG: Int? = null,

    var carbsG: Int? = null,

    var fatG: Int? = null,

    @Column(name = "food_product_id")
    var foodProductId: UUID? = null,

    var grams: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "food_category", length = 32)
    var foodCategory: FoodCategory? = null
)
