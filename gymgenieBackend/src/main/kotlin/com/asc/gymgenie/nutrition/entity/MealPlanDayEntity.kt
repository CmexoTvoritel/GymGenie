package com.asc.gymgenie.nutrition.entity

import jakarta.persistence.*
import java.time.DayOfWeek
import java.util.*

@Entity
@Table(name = "meal_plan_days")
class MealPlanDayEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    var mealPlan: MealPlanEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var dayOfWeek: DayOfWeek,

    @OneToMany(mappedBy = "mealPlanDay", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    var meals: MutableList<MealEntity> = mutableListOf()
)
