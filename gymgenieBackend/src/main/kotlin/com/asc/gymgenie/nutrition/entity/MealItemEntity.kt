package com.asc.gymgenie.nutrition.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "meal_items")
class MealItemEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    var meal: MealEntity,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 50)
    var portionSize: String? = null,

    @Column(length = 20)
    var portionUnit: String? = null,

    var calories: Int = 0,

    var proteinG: Double = 0.0,

    var fatG: Double = 0.0,

    var carbsG: Double = 0.0,

    @Column(length = 500)
    var iconUrl: String? = null,

    @Column(nullable = false)
    var orderIndex: Int = 0
)
