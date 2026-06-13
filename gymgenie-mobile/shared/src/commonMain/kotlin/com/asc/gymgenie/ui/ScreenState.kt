package com.asc.gymgenie.ui

sealed class ScreenState {
    data object Loading : ScreenState()
    data class Error(val message: String) : ScreenState()
    data object Content : ScreenState()

    val isLoading: Boolean get() = this is Loading
    val isContent: Boolean get() = this is Content
    val asError: Error? get() = this as? Error
}
