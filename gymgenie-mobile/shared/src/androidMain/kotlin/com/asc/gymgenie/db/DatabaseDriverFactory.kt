package com.asc.gymgenie.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android driver factory backed by [AndroidSqliteDriver].
 *
 * The [context] is expected to be the application context — pass it via DI
 * from the Application/Activity layer; do not hold an Activity reference here.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = GymGenieDatabase.Schema,
            context = context.applicationContext,
            name = DATABASE_NAME,
        )

    private companion object {
        const val DATABASE_NAME = "gymgenie.db"
    }
}
