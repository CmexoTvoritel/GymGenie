package com.asc.gymgenie.di

import com.asc.gymgenie.db.DatabaseDriverFactory
import com.asc.gymgenie.workout.LocalWorkoutRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android persistence singletons.
 *
 * The application context is supplied by [androidContext] in
 * `Application.onCreate()` — only the application context is held, so this
 * is safe across configuration changes and process lifecycle.
 */
actual val databaseModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { LocalWorkoutRepository(get()) }
}
