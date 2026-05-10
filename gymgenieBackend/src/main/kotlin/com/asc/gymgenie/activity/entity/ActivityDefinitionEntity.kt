package com.asc.gymgenie.activity.entity

import jakarta.persistence.*
import java.util.*

/**
 * Global catalog entry describing a habit/activity the user can pick up.
 *
 * One row per activity (not per user) — the per-user plan lives in [UserActivityEntity].
 *
 * [slug] is a stable machine identifier used by the seeder for idempotent upserts;
 * [presets] is stored as a comma-separated string of integers (null for [ActivityKind.BINARY]).
 */
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

    /** Stable machine identifier, used by the catalog seeder for idempotency. */
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

    /** Comma-separated list of preset integers (e.g. "15,30,45,60"); null for BINARY. */
    @Column(length = 100)
    var presets: String? = null,

    /** Display unit for the activity value (e.g. "мин", "стак.", "порц."); null for BINARY. */
    @Column(length = 32)
    var unit: String? = null,

    /** Default goal value applied unless the user overrides it on their plan; null for BINARY. */
    var defaultGoal: Int? = null,

    /** When true, the goal is to *avoid* the activity (e.g. no alcohol). */
    @Column(nullable = false)
    var inverse: Boolean = false,

    /** Catalog ordering hint — lower comes first. */
    @Column(nullable = false)
    var sortOrder: Int = 0
)
