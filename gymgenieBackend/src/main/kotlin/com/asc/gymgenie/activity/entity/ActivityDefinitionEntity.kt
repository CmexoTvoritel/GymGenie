package com.asc.gymgenie.activity.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "activity_definitions",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_activity_definitions_slug", columnNames = ["slug"])
    ]
)
class ActivityDefinitionEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 50)
    var slug: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var ring: ActivityRing,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var kind: ActivityKind,

    @Column(length = 100)
    var presets: String? = null,

    @Column(length = 32)
    var unit: String? = null,

    var defaultGoal: Int? = null,

    @Column(nullable = false)
    var inverse: Boolean = false,

    @Column(nullable = false)
    var sortOrder: Int = 0
)
