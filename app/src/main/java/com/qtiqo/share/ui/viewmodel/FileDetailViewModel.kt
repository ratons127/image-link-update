package com.qtiqo.share.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtiqo.share.data.local.entity.UploadEntity
import com.qtiqo.share.data.repo.UploadRepository
import com.qtiqo.share.domain.model.FilePrivacy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FileDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val uploadRepository: UploadRepository
) : ViewModel() {
    private val fileId: String = checkNotNull(savedStateHandle["fileId"])

    val file: StateFlow<UploadEntity?> = uploadRepository.observeUpload(fileId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleDownload(enabled: Boolean) {
        viewModelScope.launch { uploadRepository.updateDownloadEnabled(fileId, enabled) }
    }

    fun setPrivacy(privacy: FilePrivacy) {
        viewModelScope.launch { uploadRepository.updatePrivacy(fileId, privacy) }
    }

    fun revoke() {
        viewModelScope.launch { uploadRepository.revokeLink(fileId) }
    }

    fun regenerate() {
        viewModelScope.launch { uploadRepository.regenerateLink(fileId) }
    }

    fun retryUpload() = uploadRepository.retryUpload(fileId)
    fun cancelUpload() = uploadRepository.cancelUpload(fileId)

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            uploadRepository.deleteUpload(fileId)
            onDone()
        }
    }
}
