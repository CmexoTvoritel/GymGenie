package com.asc.gymgenie.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SessionManager {
    private val _logoutEvent = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    fun triggerLogout() {
        _logoutEvent.tryEmit(Unit)
    }

    fun observeLogout(onLogout: () -> Unit): SessionSubscription {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job: Job = scope.launch {
            logoutEvent.collect { onLogout() }
        }
        return SessionSubscription(scope, job)
    }
}

class SessionSubscription internal constructor(
    private val scope: CoroutineScope,
    private val job: Job,
) {
    fun cancel() {
        job.cancel()
        scope.coroutineContext[Job]?.cancel()
    }
}
