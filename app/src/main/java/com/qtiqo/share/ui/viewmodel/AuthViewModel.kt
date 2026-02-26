package com.qtiqo.share.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtiqo.share.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, info = null)
    }

    fun signIn(identifier: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            authRepository.login(identifier, password)
                .onSuccess { _uiState.value = AuthUiState(info = "Signed in") }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Sign in failed") }
        }
    }

    fun signUp(identifier: String, password: String, phone: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            authRepository.signUp(identifier, password, phone)
                .onSuccess { _uiState.value = AuthUiState(info = "Account created") }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Sign up failed") }
        }
    }

    fun forgot(identifier: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            authRepository.forgot(identifier)
                .onSuccess { _uiState.value = AuthUiState(info = "Reset request submitted") }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Request failed") }
        }
    }
}
