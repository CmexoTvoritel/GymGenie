package com.asc.gymgenie.di

import com.asc.gymgenie.db.DatabaseDriverFactory
import com.asc.gymgenie.workout.LocalWorkoutRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { LocalWorkoutRepository(get()) }
}
