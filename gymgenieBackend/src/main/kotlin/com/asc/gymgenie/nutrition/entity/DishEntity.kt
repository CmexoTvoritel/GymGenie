package com.asc.gymgenie.nutrition.entity

import jakarta.persistence.*
import java.util.*

/**
 * A concrete dish/food item composing a [MealEntity].
 *
 * Macro fields are nullable Ints by design: GigaChat is allowed to skip them
 * when it cannot reliably estimate a value, and we prefer a `null` macro
 * over a fabricated one. Persistence does not derive macros from any
 * catalog — the AI freely produces dish names and parameters.
 */
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

    /**
     * Free-form portion description (e.g. "150г", "1 стакан", "2 куска").
     * Stored as text because the AI does not commit to a single unit system.
     */
    @Column(length = 50)
    var portionDescription: String? = null,

    var calories: Int? = null,

    var proteinG: Int? = null,

    var carbsG: Int? = null,

    var fatG: Int? = null
)
