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

/**
 * Builds the single authenticated [HttpClient] used by every authorised API.
 *
 * Important: this function should only be invoked once per process. The
 * resulting client is wired as a Koin singleton in `AppModule.networkModule`,
 * and every call site (UserApi, WorkoutApi, AiApi, ...) must obtain it from
 * Koin instead of creating its own. Multiple clients share the same backing
 * [TokenStorage] but each holds its own in-memory `BearerTokens` cache, and
 * the moment one of them refreshes the token the others are guaranteed to
 * race against an already-rotated refresh token.
 *
 * Refresh policy:
 * - Reads the refresh token from [TokenStorage] first so a refresh that was
 *   completed by another in-flight request (or a previous launch) is observed
 *   immediately. Falls back to the Ktor-provided `oldTokens` only when the
 *   storage was cleared mid-request.
 * - On any failure path the storage is cleared and [SessionManager.triggerLogout]
 *   is fired so the UI layer can navigate back to the login screen. Returning
 *   `null` from `refreshTokens` makes Ktor surface the original 401 to the
 *   caller; the [SessionManager] event is what actually drives the user out.
 */
fun createAuthenticatedClient(
    tokenStorage: TokenStorage,
    authApi: AuthApi,
    sessionManager: SessionManager,
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
                    // Storage is the single source of truth — read it first so
                    // we always rotate against the freshest refresh token,
                    // even if Ktor's in-memory cache (oldTokens) is stale due
                    // to another request that already rotated it.
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
