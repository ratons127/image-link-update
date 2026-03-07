package com.qtiqo.share.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtiqo.share.data.local.entity.UploadEntity
import com.qtiqo.share.data.repo.UploadRepository
import com.qtiqo.share.domain.model.FilePrivacy
import com.qtiqo.share.util.persistReadPermission
import com.qtiqo.share.util.resolveFileMeta
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UploadUiState(
    val items: List<UploadEntity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val uploadRepository: UploadRepository
) : ViewModel() {
    private val _error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState: StateFlow<UploadUiState> = combine(
        uploadRepository.observeUploads(),
        _error
    ) { items, error ->
        UploadUiState(items = items, error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UploadUiState())

    fun onFilePicked(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.persistReadPermission(uri)
                val meta = resolveFileMeta(context, uri)
                uploadRepository.createQueuedUpload(
                    uri = meta.uri,
                    fileName = meta.name,
                    mimeType = meta.mimeType,
                    sizeBytes = meta.sizeBytes,
                    privacy = FilePrivacy.PUBLIC,
                    downloadEnabled = true
                )
            }.onFailure { _error.value = it.message ?: "Failed to queue upload" }
        }
    }

    fun clearError() { _error.value = null }
    fun cancelUpload(id: String) {
        viewModelScope.launch { uploadRepository.cancelAndRemoveUpload(id) }
    }
    fun retryUpload(id: String) = uploadRepository.retryUpload(id)
    fun deleteUpload(id: String) {
        viewModelScope.launch { uploadRepository.deleteUpload(id) }
    }
}
