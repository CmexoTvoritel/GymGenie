package com.asc.gymgenie.di

import com.asc.gymgenie.db.DatabaseDriverFactory
import com.asc.gymgenie.workout.LocalWorkoutRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
    single { DatabaseDriverFactory() }
    single { LocalWorkoutRepository(get()) }
}
