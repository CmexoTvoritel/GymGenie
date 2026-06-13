package com.asc.gymgenie.common

import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.storage.TokenStorage
import io.ktor.client.HttpClient
import com.asc.gymgenie.config.AppConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun HttpClient.clearBearerTokens() {
    authProvider<BearerAuthProvider>()?.clearToken()
}

fun clearHttpClientBearerTokens(client: HttpClient) {
    client.clearBearerTokens()
}

fun createAuthenticatedClient(
    tokenStorage: TokenStorage,
    authApi: AuthApi,
    sessionManager: SessionManager,
): HttpClient {
    return HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 120_000
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val accessToken = tokenStorage.getAccessToken()
                    val refreshToken = tokenStorage.getRefreshToken()
                    if (accessToken != null && refreshToken != null) {
                        BearerTokens(accessToken, refreshToken)
                    } else {
                        null
                    }
                }

                refreshTokens {
                    val refreshToken = tokenStorage.getRefreshToken()
                        ?: oldTokens?.refreshToken

                    if (refreshToken == null) {
                        tokenStorage.clearTokens()
                        sessionManager.triggerLogout()
                        return@refreshTokens null
                    }

                    val result = authApi.refreshToken(refreshToken)
                    result.fold(
                        onSuccess = { tokenResponse ->
                            tokenStorage.saveTokens(
                                accessToken = tokenResponse.accessToken,
                                refreshToken = tokenResponse.refreshToken,
                            )
                            BearerTokens(
                                tokenResponse.accessToken,
                                tokenResponse.refreshToken,
                            )
                        },
                        onFailure = {
                            tokenStorage.clearTokens()
                            sessionManager.triggerLogout()
                            null
                        },
                    )
                }

                sendWithoutRequest { request ->
                    request.url.host == Url(AppConfig.BASE_URL).host
                }
            }
        }
    }
}
