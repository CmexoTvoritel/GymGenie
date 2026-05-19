package com.asc.gymgenie.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

@Suppress("EXPECT_ACTUAL_CLASSES")
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(
            schema = GymGenieDatabase.Schema,
            name = DATABASE_NAME,
        )
        migrateIfNeeded(driver)
        return driver
    }

    private fun migrateIfNeeded(driver: SqlDriver) {
        val columns = driver.executeQuery(null, "PRAGMA table_info(PendingWorkoutSession)", { cursor ->
            val cols = mutableSetOf<String>()
            while (cursor.next().value) {
                cursor.getString(1)?.let { cols.add(it) }
            }
            QueryResult.Value(cols)
        }, 0).value

        if ("finished_at" !in columns) {
            try {
                driver.execute(null, "ALTER TABLE PendingWorkoutSession ADD COLUMN finished_at INTEGER", 0)
            } catch (_: Exception) { }
        }
        if ("status" !in columns) {
            try {
                driver.execute(null, "ALTER TABLE PendingWorkoutSession ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'", 0)
            } catch (_: Exception) { }
        }
    }

    private companion object {
        const val DATABASE_NAME = "gymgenie.db"
    }
}
