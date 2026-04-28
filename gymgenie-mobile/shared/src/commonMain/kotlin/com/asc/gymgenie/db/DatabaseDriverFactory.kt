package com.asc.gymgenie.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific factory for opening the underlying SQLite driver used by
 * [GymGenieDatabase].
 *
 * Lives in shared code so the local persistence layer can be wired without
 * leaking platform types into the rest of the codebase.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
