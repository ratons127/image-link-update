package com.qtiqo.share.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtiqo.share.data.local.entity.UploadEntity
import com.qtiqo.share.data.repo.UploadRepository
import com.qtiqo.share.domain.model.FilePrivacy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GalleryFilter { ALL, PUBLIC, UNLISTED, PRIVATE }
enum class GallerySort { NEWEST, OLDEST, LARGEST, SMALLEST }

data class GalleryUiState(
    val search: String = "",
    val filter: GalleryFilter = GalleryFilter.ALL,
    val sort: GallerySort = GallerySort.NEWEST,
    val items: List<UploadEntity> = emptyList()
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val uploadRepository: UploadRepository
) : ViewModel() {
    private val search = MutableStateFlow("")
    private val filter = MutableStateFlow(GalleryFilter.ALL)
    private val sort = MutableStateFlow(GallerySort.NEWEST)

    val uiState: StateFlow<GalleryUiState> = combine(
        uploadRepository.observeUploads(),
        search,
        filter,
        sort
    ) { items, search, filter, sort ->
        var filtered = items
        if (search.isNotBlank()) {
            val q = search.trim().lowercase()
            filtered = filtered.filter { it.fileName.lowercase().contains(q) || (it.mimeType ?: "").lowercase().contains(q) }
        }
        filtered = when (filter) {
            GalleryFilter.ALL -> filtered
            GalleryFilter.PUBLIC -> filtered.filter { it.privacy == FilePrivacy.PUBLIC }
            GalleryFilter.UNLISTED -> filtered.filter { it.privacy == FilePrivacy.UNLISTED }
            GalleryFilter.PRIVATE -> filtered.filter { it.privacy == FilePrivacy.PRIVATE }
        }
        filtered = when (sort) {
            GallerySort.NEWEST -> filtered.sortedByDescending { it.createdAt }
            GallerySort.OLDEST -> filtered.sortedBy { it.createdAt }
            GallerySort.LARGEST -> filtered.sortedByDescending { it.sizeBytes }
            GallerySort.SMALLEST -> filtered.sortedBy { it.sizeBytes }
        }
        GalleryUiState(search = search, filter = filter, sort = sort, items = filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState())

    fun setSearch(value: String) { search.value = value }
    fun setFilter(value: GalleryFilter) { filter.value = value }
    fun setSort(value: GallerySort) { sort.value = value }

    fun cancelUpload(id: String) {
        viewModelScope.launch { uploadRepository.cancelAndRemoveUpload(id) }
    }
    fun retryUpload(id: String) = uploadRepository.retryUpload(id)

    fun deleteUpload(id: String) {
        viewModelScope.launch { uploadRepository.deleteUpload(id) }
    }
}
