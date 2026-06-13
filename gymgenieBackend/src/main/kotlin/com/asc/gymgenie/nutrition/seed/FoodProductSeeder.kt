package com.asc.gymgenie.nutrition.seed

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
@Order(200)
class FoodProductSeeder(
    private val jdbcTemplate: JdbcTemplate
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(FoodProductSeeder::class.java)

    override fun run(vararg args: String) {
        val existing = countExisting()
        if (existing == null) {
            log.warn("Skipping food products seed: unable to query food_products table.")
            return
        }
        if (existing > 0) {
            log.debug("Skipping food products seed: catalog already populated ({} rows).", existing)
            return
        }

        val script = loadScript()
        if (script == null) {
            log.error("Food products seed script not found at classpath:{}", SEED_SCRIPT_PATH)
            return
        }

        try {
            jdbcTemplate.execute(script)
        } catch (ex: Exception) {
            log.error("Food products seed failed to execute.", ex)
            return
        }

        val inserted = countExisting() ?: 0
        log.info("Food products catalog seeded: {} rows inserted.", inserted)
    }

    private fun countExisting(): Long? = try {
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM food_products", Long::class.java)
    } catch (ex: Exception) {
        log.warn("Could not count food_products rows: {}", ex.message)
        null
    }

    private fun loadScript(): String? {
        val resource = ClassPathResource(SEED_SCRIPT_PATH)
        if (!resource.exists()) return null
        return resource.inputStream.use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8)
        }
    }

    private companion object {
        const val SEED_SCRIPT_PATH = "db/food_products_seed.sql"
    }
}
