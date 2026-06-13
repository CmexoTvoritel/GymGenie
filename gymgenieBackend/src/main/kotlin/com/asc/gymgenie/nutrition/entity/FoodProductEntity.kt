package com.asc.gymgenie.nutrition.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "food_products")
class FoodProductEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 150)
    var nameRu: String,

    @Column(length = 150)
    var nameEn: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var category: FoodCategory,

    @Column(length = 10)
    var emoji: String? = null,

    @Column(nullable = false)
    var caloriesPer100g: Double,

    @Column(nullable = false)
    var proteinPer100g: Double,

    @Column(nullable = false)
    var fatPer100g: Double,

    @Column(nullable = false)
    var carbsPer100g: Double,

    var fiberPer100g: Double? = null,

    var sugarPer100g: Double? = null,

    @Column(nullable = false)
    var isActive: Boolean = true
)
