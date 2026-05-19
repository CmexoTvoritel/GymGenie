package com.asc.gymgenie.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

@Suppress("EXPECT_ACTUAL_CLASSES")
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(
            schema = GymGenieDatabase.Schema,
            context = context.applicationContext,
            name = DATABASE_NAME,
        )
        migrateIfNeeded(driver)
        return driver
    }

    private fun migrateIfNeeded(driver: SqlDriver) {
        try {
            driver.execute(null, "ALTER TABLE PendingWorkoutSession ADD COLUMN finished_at INTEGER", 0)
        } catch (_: Exception) {
        }
        try {
            driver.execute(null, "ALTER TABLE PendingWorkoutSession ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'", 0)
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val DATABASE_NAME = "gymgenie.db"
    }
}
