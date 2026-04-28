package com.asc.gymgenie.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS driver factory backed by [NativeSqliteDriver].
 *
 * No platform context is required — the native driver opens a sqlite file in
 * the app's documents directory using [DATABASE_NAME].
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(
            schema = GymGenieDatabase.Schema,
            name = DATABASE_NAME,
        )

    private companion object {
        const val DATABASE_NAME = "gymgenie.db"
    }
}
