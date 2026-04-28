package com.asc.gymgenie.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gigachat")
data class GigaChatProperties(
    val authKey: String
)
