package com.asc.gymgenie.workout.entity

enum class CreatedBy {
    AI, USER
}

enum class SessionStatus {
    IN_PROGRESS, COMPLETED, CANCELLED
}

enum class WorkoutScheduleType {
    ONE_TIME, RECURRING
}
