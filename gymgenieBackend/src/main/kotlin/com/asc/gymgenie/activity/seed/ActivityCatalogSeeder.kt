package com.asc.gymgenie.activity.seed

import com.asc.gymgenie.activity.entity.ActivityDefinitionEntity
import com.asc.gymgenie.activity.entity.ActivityKind
import com.asc.gymgenie.activity.entity.ActivityRing
import com.asc.gymgenie.activity.repository.ActivityDefinitionRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(100)
class ActivityCatalogSeeder(
    private val activityDefinitionRepository: ActivityDefinitionRepository
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(ActivityCatalogSeeder::class.java)

    @Transactional
    override fun run(vararg args: String) {
        var inserted = 0
        var updated = 0

        SEED.forEachIndexed { index, seed ->
            val sortOrder = index
            val existing = activityDefinitionRepository.findBySlug(seed.slug)
            if (existing == null) {
                activityDefinitionRepository.save(
                    ActivityDefinitionEntity(
                        slug = seed.slug,
                        name = seed.name,
                        ring = seed.ring,
                        kind = seed.kind,
                        presets = seed.presets?.joinToString(","),
                        unit = seed.unit,
                        defaultGoal = seed.defaultGoal,
                        inverse = seed.inverse,
                        sortOrder = sortOrder
                    )
                )
                inserted++
            } else {
                val newPresets = seed.presets?.joinToString(",")
                val needsUpdate = existing.name != seed.name ||
                    existing.ring != seed.ring ||
                    existing.kind != seed.kind ||
                    existing.presets != newPresets ||
                    existing.unit != seed.unit ||
                    existing.defaultGoal != seed.defaultGoal ||
                    existing.inverse != seed.inverse ||
                    existing.sortOrder != sortOrder
                if (needsUpdate) {
                    existing.name = seed.name
                    existing.ring = seed.ring
                    existing.kind = seed.kind
                    existing.presets = newPresets
                    existing.unit = seed.unit
                    existing.defaultGoal = seed.defaultGoal
                    existing.inverse = seed.inverse
                    existing.sortOrder = sortOrder
                    activityDefinitionRepository.save(existing)
                    updated++
                }
            }
        }

        if (inserted > 0 || updated > 0) {
            log.info("Activity catalog seed: {} inserted, {} updated", inserted, updated)
        }
    }

    private data class SeedRow(
        val slug: String,
        val name: String,
        val ring: ActivityRing,
        val kind: ActivityKind,
        val presets: List<Int>? = null,
        val unit: String? = null,
        val defaultGoal: Int? = null,
        val inverse: Boolean = false
    )

    companion object {
        private val SEED: List<SeedRow> = listOf(

            SeedRow("walk",       "Прогулка",            ActivityRing.MOVE, ActivityKind.PRESET,  listOf(15, 30, 45, 60), "мин",   30),
            SeedRow("morning",    "Утренняя зарядка",    ActivityRing.MOVE, ActivityKind.BINARY),
            SeedRow("stretch",    "Растяжка",            ActivityRing.MOVE, ActivityKind.BINARY),
            SeedRow("run",        "Бег",                 ActivityRing.MOVE, ActivityKind.PRESET,  listOf(15, 20, 30, 45), "мин",   20),
            SeedRow("bike",       "Велосипед",           ActivityRing.MOVE, ActivityKind.BINARY),
            SeedRow("swim",       "Плавание",            ActivityRing.MOVE, ActivityKind.BINARY),

            SeedRow("meditate",   "Медитация",           ActivityRing.MIND, ActivityKind.PRESET,  listOf(5, 10, 15, 20),  "мин",   10),
            SeedRow("breath",     "Дыхательная практика",ActivityRing.MIND, ActivityKind.BINARY),
            SeedRow("read",       "Чтение",              ActivityRing.MIND, ActivityKind.PRESET,  listOf(15, 30, 45, 60), "мин",   30),
            SeedRow("journal",    "Дневник",             ActivityRing.MIND, ActivityKind.BINARY),
            SeedRow("nophone",    "Без телефона",        ActivityRing.MIND, ActivityKind.BINARY),
            SeedRow("sleep",      "Сон 8 часов",         ActivityRing.MIND, ActivityKind.BINARY),

            SeedRow("water",      "Вода",                ActivityRing.LIFE, ActivityKind.COUNTER, null, "стак.", 8),
            SeedRow("vitamins",   "Витамины",            ActivityRing.LIFE, ActivityKind.BINARY),
            SeedRow("shower",     "Контрастный душ",     ActivityRing.LIFE, ActivityKind.BINARY),
            SeedRow("wakeup",     "Ранний подъём",       ActivityRing.LIFE, ActivityKind.BINARY),
            SeedRow("breakfast",  "Полезный завтрак",    ActivityRing.LIFE, ActivityKind.BINARY),
            SeedRow("veg",        "Овощи",               ActivityRing.LIFE, ActivityKind.COUNTER, null, "порц.", 2),
            SeedRow("noalc",      "Без алкоголя",        ActivityRing.LIFE, ActivityKind.BINARY,  inverse = true),
            SeedRow("nosugar",    "Без сахара",          ActivityRing.LIFE, ActivityKind.BINARY),
            SeedRow("earlysleep", "Ранний отбой",        ActivityRing.LIFE, ActivityKind.BINARY),
            SeedRow("mfr",        "МФР / массаж",        ActivityRing.LIFE, ActivityKind.BINARY)
        )
    }
}
