package com.asc.gymgenie.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

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

    /**
     * Swift-friendly subscription helper.
     *
     * iOS wrappers cannot directly call [SharedFlow.collect] from Swift
     * because Kotlin `Flow.collect` has no `value` snapshot to poll. This
     * helper bridges the SharedFlow into a plain callback and returns a
     * cancellation handle the caller invokes from `deinit`.
     *
     * On Android the SharedFlow is consumed directly with `.collect { ... }`
     * inside a `LaunchedEffect`, so this helper is unused there.
     */
    fun observeLogout(onLogout: () -> Unit): SessionSubscription {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job: Job = scope.launch {
            logoutEvent.collect { onLogout() }
        }
        return SessionSubscription(scope, job)
    }
}

/**
 * Cancellation handle for [SessionManager.observeLogout]. Calling [cancel]
 * stops the underlying coroutine so the callback no longer fires.
 */
class SessionSubscription internal constructor(
    private val scope: CoroutineScope,
    private val job: Job,
) {
    fun cancel() {
        job.cancel()
        scope.coroutineContext[Job]?.cancel()
    }
}
