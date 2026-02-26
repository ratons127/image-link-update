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

@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {
    val session: StateFlow<UserSession?> = authRepository.sessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.sessionFlow.value)
}
