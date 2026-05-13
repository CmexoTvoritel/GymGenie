package com.asc.gymgenie.di

import org.koin.core.module.Module

/**
 * Koin module that exposes the local persistence layer.
 *
 * Defined per-platform because the Android driver requires an application
 * `Context`, while the iOS driver opens a sqlite file by name with no
 * platform context. The common module simply declares the shape; each
 * platform binds [com.asc.gymgenie.db.DatabaseDriverFactory] and
 * [com.asc.gymgenie.workout.LocalWorkoutRepository] as singletons.
 */
expect val databaseModule: Module
