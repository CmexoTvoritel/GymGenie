package com.asc.gymgenie.ai.client.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GigaChatMessage(
    val role: String,
    val content: String
)

data class GigaChatChatRequest(
    val model: String,
    val messages: List<GigaChatMessage>,
    val temperature: Double = 0.3,
    val stream: Boolean = false
)

data class GigaChatChatResponse(
    val choices: List<GigaChatChoice>
)

data class GigaChatChoice(
    val message: GigaChatMessage,
    @JsonProperty("finish_reason")
    val finishReason: String?
)

data class GigaChatTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_at")
    val expiresAt: Long
)
