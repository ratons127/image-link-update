package com.qtiqo.share.feature.profile.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtiqo.share.data.profile.repo.ProfileRepository
import com.qtiqo.share.feature.profile.domain.models.ProfileSummary
import com.qtiqo.share.feature.profile.integration.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ProfileSummaryUiState {
    data object Loading : ProfileSummaryUiState
    data class Success(val summary: ProfileSummary) : ProfileSummaryUiState
    data class Error(val message: String) : ProfileSummaryUiState
}

sealed interface ProfileEvent {
    data class Snackbar(val message: String) : ProfileEvent
    data object NavigateToLogin : ProfileEvent
}

data class PasswordFormState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentError: String? = null,
    val newError: String? = null,
    val confirmError: String? = null,
    val isSubmitting: Boolean = false
) {
    val isValid: Boolean =
        currentPassword.isNotBlank() &&
            newPassword.length >= 8 &&
            confirmPassword == newPassword &&
            currentError == null &&
            newError == null &&
            confirmError == null &&
            !isSubmitting
}

data class ProfileScreenState(
    val sessionEmail: String? = null,
    val summaryState: ProfileSummaryUiState = ProfileSummaryUiState.Loading,
    val passwordForm: PasswordFormState = PasswordFormState(),
    val signingOut: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    sessionManager: SessionManager
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionManager.sessionFlow.collect { session ->
                _state.update { it.copy(sessionEmail = session?.email) }
            }
        }
        loadSummary()
    }

    fun loadSummary() = viewModelScope.launch {
        _state.update { it.copy(summaryState = ProfileSummaryUiState.Loading) }
        runCatching { repository.getSummary() }
            .onSuccess { _state.update { s -> s.copy(summaryState = ProfileSummaryUiState.Success(it)) } }
            .onFailure {
                _state.update { s -> s.copy(summaryState = ProfileSummaryUiState.Error(it.message ?: "Failed to load profile")) }
                _events.send(ProfileEvent.Snackbar(it.message ?: "Failed to load profile"))
            }
    }

    fun onCurrentPasswordChanged(value: String) {
        _state.update { s ->
            s.copy(passwordForm = validate(s.passwordForm.copy(currentPassword = value)))
        }
    }

    fun onNewPasswordChanged(value: String) {
        _state.update { s ->
            s.copy(passwordForm = validate(s.passwordForm.copy(newPassword = value)))
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _state.update { s ->
            s.copy(passwordForm = validate(s.passwordForm.copy(confirmPassword = value)))
        }
    }

    fun updatePassword() = viewModelScope.launch {
        val form = validate(_state.value.passwordForm)
        _state.update { it.copy(passwordForm = form) }
        if (!form.isValid) return@launch

        _state.update { it.copy(passwordForm = it.passwordForm.copy(isSubmitting = true)) }
        runCatching { repository.changePassword(form.currentPassword, form.newPassword) }
            .onSuccess {
                _state.update {
                    it.copy(
                        passwordForm = PasswordFormState()
                    )
                }
                _events.send(ProfileEvent.Snackbar("Password updated successfully"))
            }
            .onFailure {
                _state.update { s ->
                    s.copy(passwordForm = s.passwordForm.copy(isSubmitting = false))
                }
                _events.send(ProfileEvent.Snackbar(it.message ?: "Failed to update password"))
            }
    }

    fun signOut() = viewModelScope.launch {
        _state.update { it.copy(signingOut = true) }
        runCatching { repository.logout() }
            .onSuccess {
                _state.update { it.copy(signingOut = false) }
                _events.send(ProfileEvent.Snackbar("Signed out"))
                _events.send(ProfileEvent.NavigateToLogin)
            }
            .onFailure {
                _state.update { it.copy(signingOut = false) }
                _events.send(ProfileEvent.Snackbar(it.message ?: "Sign out failed"))
            }
    }

    private fun validate(form: PasswordFormState): PasswordFormState {
        val currentError = if (form.currentPassword.isBlank()) "Required" else null
        val newError = when {
            form.newPassword.isBlank() -> "Required"
            form.newPassword.length < 8 -> "Minimum 8 characters"
            else -> null
        }
        val confirmError = when {
            form.confirmPassword.isBlank() -> "Required"
            form.confirmPassword != form.newPassword -> "Passwords do not match"
            else -> null
        }
        return form.copy(
            currentError = currentError,
            newError = newError,
            confirmError = confirmError
        )
    }
}
