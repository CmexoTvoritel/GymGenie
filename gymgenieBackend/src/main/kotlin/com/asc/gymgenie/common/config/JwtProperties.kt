package com.asc.gymgenie.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpiration: Duration = Duration.ofMinutes(15),
    val refreshTokenExpiration: Duration = Duration.ofDays(30)
)
