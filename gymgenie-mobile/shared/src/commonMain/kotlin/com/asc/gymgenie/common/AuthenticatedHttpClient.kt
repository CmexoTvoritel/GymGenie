package com.asc.gymgenie.common

import com.asc.gymgenie.auth.AuthApi
import com.asc.gymgenie.storage.TokenStorage
import io.ktor.client.HttpClient
import com.asc.gymgenie.config.AppConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createAuthenticatedClient(
    tokenStorage: TokenStorage,
    authApi: AuthApi,
): HttpClient {
    return HttpClient {
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
                    val refreshToken = oldTokens?.refreshToken
                        ?: tokenStorage.getRefreshToken()

                    if (refreshToken == null) {
                        tokenStorage.clearTokens()
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
