package com.asc.gymgenie.ai.client

import com.asc.gymgenie.ai.client.dto.GigaChatChatRequest
import com.asc.gymgenie.ai.client.dto.GigaChatChatResponse
import com.asc.gymgenie.ai.client.dto.GigaChatMessage
import com.asc.gymgenie.common.exception.BadRequestException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class GigaChatClient(
    @Qualifier("gigaChatRestClient") private val restClient: RestClient,
    private val authService: GigaChatAuthService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun chat(messages: List<GigaChatMessage>): String {
        val token = authService.getAccessToken()
        val request = GigaChatChatRequest(
            model = MODEL,
            messages = messages,
            temperature = TEMPERATURE,
            stream = false
        )
        log.debug("Sending {} messages to GigaChat", messages.size)

        return try {
            val response = restClient.post()
                .uri(CHAT_URL)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GigaChatChatResponse::class.java)
                ?: throw BadRequestException("Empty response from GigaChat")

            response.choices.firstOrNull()?.message?.content
                ?: throw BadRequestException("No content in GigaChat response")
        } catch (e: RestClientResponseException) {
            log.error("GigaChat API error: {} {}", e.statusCode, e.responseBodyAsString)
            throw BadRequestException("AI service error, please try again later")
        }
    }

    companion object {
        private const val CHAT_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
        private const val MODEL = "GigaChat-2-Lite"
        private const val TEMPERATURE = 0.3
    }
}
