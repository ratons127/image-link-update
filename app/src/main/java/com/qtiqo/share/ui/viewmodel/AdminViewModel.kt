package com.qtiqo.share.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtiqo.share.data.repo.AuthRepository
import com.qtiqo.share.data.repo.UploadRepository
import com.qtiqo.share.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AdminUiState(
    val authorized: Boolean = false,
    val users: List<Pair<String, String>> = emptyList(),
    val statsUsers: Int = 0,
    val statsFiles: Int = 0,
    val statsStorageBytes: Long = 0,
    val statsViews: Int = 0
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    authRepository: AuthRepository,
    uploadRepository: UploadRepository
) : ViewModel() {
    val uiState: StateFlow<AdminUiState> = combine(
        authRepository.sessionFlow,
        uploadRepository.observeAdminStats()
    ) { session, stats ->
        val authorized = session?.role == UserRole.ADMIN
        AdminUiState(
            authorized = authorized,
            users = if (authorized) uploadRepository.fakeUsersList() else emptyList(),
            statsUsers = stats.users,
            statsFiles = stats.files,
            statsStorageBytes = stats.storageBytes,
            statsViews = stats.views
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUiState())
}
