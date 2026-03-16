package com.asc.gymgenie.common.exception

import java.time.Instant

data class ErrorResponse(
    val message: String,
    val errors: Map<String, String>? = null,
    val timestamp: Instant = Instant.now()
)
