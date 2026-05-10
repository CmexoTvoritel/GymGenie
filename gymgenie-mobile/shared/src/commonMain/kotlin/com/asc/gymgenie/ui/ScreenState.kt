package com.asc.gymgenie.ui

/**
 * Canonical "screen phase" contract for all KMM-backed screens.
 *
 * Three explicit phases keep the presentation contract uniform between Compose
 * and SwiftUI: render a full-screen loading skeleton, render a blocking error,
 * or render real content. Background refreshes (e.g. pull-to-refresh) are
 * tracked separately on the per-screen UI state to avoid collapsing back into
 * [Loading] once content has been shown at least once.
 */
sealed class ScreenState {
    data object Loading : ScreenState()
    data class Error(val message: String) : ScreenState()
    data object Content : ScreenState()

    val isLoading: Boolean get() = this is Loading
    val isContent: Boolean get() = this is Content
    val asError: Error? get() = this as? Error
}
