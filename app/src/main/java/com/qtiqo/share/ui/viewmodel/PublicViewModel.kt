package com.qtiqo.share.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtiqo.share.data.repo.UploadRepository
import com.qtiqo.share.domain.model.PublicFileView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PublicUiState(
    val loading: Boolean = true,
    val item: PublicFileView? = null,
    val error: String? = null
)

@HiltViewModel
class PublicViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val uploadRepository: UploadRepository
) : ViewModel() {
    private val token: String = checkNotNull(savedStateHandle["token"])
    private val _uiState = MutableStateFlow(PublicUiState())
    val uiState: StateFlow<PublicUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = PublicUiState(loading = true)
            uploadRepository.resolvePublicToken(token)
                .onSuccess { _uiState.value = PublicUiState(loading = false, item = it) }
                .onFailure { _uiState.value = PublicUiState(loading = false, error = it.message ?: "Unable to open link") }
        }
    }
}
