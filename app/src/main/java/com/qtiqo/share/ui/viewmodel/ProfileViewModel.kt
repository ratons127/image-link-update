package com.qtiqo.share.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtiqo.share.data.repo.AuthRepository
import com.qtiqo.share.domain.model.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    val session: StateFlow<UserSession?> = authRepository.sessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.sessionFlow.value)

    val useFakeBackend: StateFlow<Boolean> = authRepository.useFakeBackend
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.useFakeBackend.value)

    fun setFakeBackend(enabled: Boolean) = authRepository.setFakeBackend(enabled)

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
