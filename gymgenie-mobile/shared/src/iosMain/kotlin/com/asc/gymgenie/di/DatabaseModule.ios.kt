package com.asc.gymgenie.di

import com.asc.gymgenie.db.DatabaseDriverFactory
import com.asc.gymgenie.workout.LocalWorkoutRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS persistence singletons. The native driver opens a sqlite file by
 * name in the app's documents directory and needs no platform context.
 */
actual val databaseModule: Module = module {
    single { DatabaseDriverFactory() }
    single { LocalWorkoutRepository(get()) }
}
