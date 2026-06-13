package com.asc.gymgenie.db

import app.cash.sqldelight.db.SqlDriver

@Suppress("EXPECT_ACTUAL_CLASSES")
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
