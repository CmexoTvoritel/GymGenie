package com.asc.gymgenie.ai.client

import com.asc.gymgenie.ai.client.dto.GigaChatTokenResponse
import com.asc.gymgenie.ai.config.GigaChatProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class GigaChatAuthService(
    private val properties: GigaChatProperties,
    @Qualifier("gigaChatRestClient") private val restClient: RestClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiresAt: Long = 0L

    @Synchronized
    fun getAccessToken(): String {
        val now = System.currentTimeMillis()
        val token = cachedToken
        if (token != null && now < tokenExpiresAt - TOKEN_REFRESH_MARGIN_MS) {
            return token
        }
        log.debug("Fetching new GigaChat access token")
        return fetchNewToken()
    }

    private fun fetchNewToken(): String {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("scope", "GIGACHAT_API_PERS")
        }
        val response = restClient.post()
            .uri(AUTH_URL)
            .header("Authorization", "Basic ${properties.authKey}")
            .header("RqUID", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(GigaChatTokenResponse::class.java)
            ?: error("Empty response from GigaChat auth endpoint")

        cachedToken = response.accessToken
        // GigaChat returns expires_at as Unix timestamp in milliseconds
        tokenExpiresAt = response.expiresAt
        log.debug("GigaChat token refreshed, expires at {}", tokenExpiresAt)
        return response.accessToken
    }

    companion object {
        private const val AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
        private const val TOKEN_REFRESH_MARGIN_MS = 60_000L
    }
}
