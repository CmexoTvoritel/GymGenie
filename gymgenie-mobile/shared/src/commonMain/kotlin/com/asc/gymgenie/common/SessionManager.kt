package com.asc.gymgenie.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide signal bus for session lifecycle events.
 *
 * The authenticated [io.ktor.client.HttpClient] is the only place that can
 * detect a definitively-invalid refresh token (the server rejected it). When
 * that happens it cannot itself navigate the user back to the login screen —
 * navigation lives in the platform layer. Instead it emits on
 * [logoutEvent] and the App composition root translates the signal into a
 * navigation reset.
 *
 * `extraBufferCapacity = 1` ensures `tryEmit` is non-blocking and never drops
 * the event when no collector is subscribed at the exact moment of emission.
 * `replay = 0` keeps stale events from re-firing on a fresh subscription —
 * each logout must be observed in real time.
 */
class SessionManager {
    private val _logoutEvent = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    fun triggerLogout() {
        _logoutEvent.tryEmit(Unit)
    }
}
