package com.qtiqo.share.feature.admin.ui.vm

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val value: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}

sealed interface AdminEvent {
    data class Snackbar(val message: String) : AdminEvent
}

enum class AdminPanelTab { USERS, CONTENT, LOGS, SETTINGS }
