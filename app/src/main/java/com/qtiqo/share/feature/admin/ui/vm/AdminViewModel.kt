package com.qtiqo.share.feature.admin.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtiqo.share.feature.admin.data.repo.AdminRepository
import com.qtiqo.share.feature.admin.domain.model.*
import com.qtiqo.share.feature.admin.integration.Session
import com.qtiqo.share.feature.admin.integration.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminRouteState(
    val isAuthorized: Boolean = false,
    val session: Session? = null,
    val selectedTab: AdminPanelTab = AdminPanelTab.USERS,
    val statsState: UiState<AdminStats> = UiState.Loading,
    val usersState: UiState<List<AdminUser>> = UiState.Loading,
    val usersNextPage: Int? = null,
    val usersQuery: String = "",
    val filesState: UiState<List<AdminFile>> = UiState.Loading,
    val filesNextPage: Int? = null,
    val filesQuery: String = "",
    val filesPrivacy: AdminPrivacy? = null,
    val fileSort: ContentSort = ContentSort.NEWEST,
    val logsState: UiState<List<AdminLog>> = UiState.Loading,
    val logsNextPage: Int? = null,
    val logEventFilter: AdminLogEventType? = null,
    val logActorFilter: String = "",
    val logTargetFilter: String = "",
    val settingsState: UiState<AdminSettings> = UiState.Loading,
    val savingSettings: Boolean = false,
    val addingUser: Boolean = false,
    val selectedFileDetail: AdminFile? = null,
    val selectedPublicPreview: AdminFile? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: AdminRepository,
    sessionManager: SessionManager
) : ViewModel() {
    private val _state = MutableStateFlow(AdminRouteState())
    val state: StateFlow<AdminRouteState> = _state.asStateFlow()

    private val _events = Channel<AdminEvent>(Channel.BUFFERED)
    val events: Flow<AdminEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionManager.sessionFlow.collect { session ->
                val authorized = session?.role.equals("ADMIN", true)
                _state.update { it.copy(session = session, isAuthorized = authorized) }
                if (authorized) refreshAll() else _state.update { s -> s.copy(statsState = UiState.Error("Not authorized")) }
            }
        }
    }

    fun selectTab(tab: AdminPanelTab) {
        _state.update { it.copy(selectedTab = tab) }
        when (tab) {
            AdminPanelTab.USERS -> if (_state.value.usersState is UiState.Loading) loadUsers(reset = true)
            AdminPanelTab.CONTENT -> if (_state.value.filesState is UiState.Loading) loadFiles(reset = true)
            AdminPanelTab.LOGS -> if (_state.value.logsState is UiState.Loading) loadLogs(reset = true)
            AdminPanelTab.SETTINGS -> if (_state.value.settingsState is UiState.Loading) loadSettings()
        }
    }

    fun refreshAll() {
        if (!_state.value.isAuthorized) return
        loadStats()
        loadUsers(reset = true)
        loadFiles(reset = true)
        loadLogs(reset = true)
        loadSettings()
    }

    fun updateUsersQuery(q: String) {
        _state.update { it.copy(usersQuery = q) }
        loadUsers(reset = true)
    }

    fun loadMoreUsers() = loadUsers(reset = false)

    private fun loadUsers(reset: Boolean) = viewModelScope.launch {
        if (!_state.value.isAuthorized) return@launch
        val currentItems = (_state.value.usersState as? UiState.Success)?.value.orEmpty()
        val nextPage = if (reset) 1 else _state.value.usersNextPage ?: return@launch
        if (reset) _state.update { it.copy(usersState = UiState.Loading) }
        runCatching {
            repository.getUsers(UserQuery(query = _state.value.usersQuery, page = nextPage, pageSize = 10))
        }.onSuccess { page ->
            val merged = if (reset) page.items else currentItems + page.items
            _state.update {
                it.copy(
                    usersState = if (merged.isEmpty()) UiState.Empty else UiState.Success(merged),
                    usersNextPage = page.nextPage
                )
            }
        }.onFailure { fail("Failed to load users: ${it.message}") { s -> s.copy(usersState = UiState.Error("Failed to load users")) } }
    }

    fun addUser(input: CreateAdminUserInput) = viewModelScope.launch {
        _state.update { it.copy(addingUser = true) }
        runCatching { repository.createUser(input) }
            .onSuccess {
                _events.send(AdminEvent.Snackbar("User created"))
                _state.update { s -> s.copy(addingUser = false) }
                loadStats()
                loadUsers(reset = true)
            }
            .onFailure {
                _state.update { s -> s.copy(addingUser = false) }
                _events.send(AdminEvent.Snackbar("Create user failed: ${it.message ?: "Unknown"}"))
            }
    }

    fun toggleUserRole(user: AdminUser) = viewModelScope.launch {
        runCatching {
            repository.updateUser(user.id, UpdateAdminUserInput(role = if (user.role == AdminRole.ADMIN) AdminRole.USER else AdminRole.ADMIN))
        }.onSuccess {
            _events.send(AdminEvent.Snackbar("User role updated"))
            loadUsers(reset = true)
        }.onFailure { _events.send(AdminEvent.Snackbar(it.message ?: "Update failed")) }
    }

    fun toggleUserSuspended(user: AdminUser) = viewModelScope.launch {
        runCatching { repository.updateUser(user.id, UpdateAdminUserInput(isSuspended = !user.isSuspended)) }
            .onSuccess {
                _events.send(AdminEvent.Snackbar(if (user.isSuspended) "User activated" else "User suspended"))
                loadUsers(reset = true)
            }
            .onFailure { _events.send(AdminEvent.Snackbar(it.message ?: "Update failed")) }
    }

    fun deleteUser(user: AdminUser) = viewModelScope.launch {
        runCatching { repository.deleteUser(user.id) }
            .onSuccess {
                _events.send(AdminEvent.Snackbar("User deleted"))
                loadStats(); loadUsers(reset = true); loadFiles(reset = true)
            }
            .onFailure { _events.send(AdminEvent.Snackbar(it.message ?: "Delete failed")) }
    }

    fun setUserLimits(user: AdminUser, storageLimitBytes: Long, maxUploadSizeBytes: Long) = viewModelScope.launch {
        runCatching {
            repository.updateUser(user.id, UpdateAdminUserInput(storageLimitBytes = storageLimitBytes, maxUploadSizeBytes = maxUploadSizeBytes))
        }.onSuccess {
            _events.send(AdminEvent.Snackbar("Limits updated"))
            loadUsers(reset = true)
        }.onFailure { _events.send(AdminEvent.Snackbar(it.message ?: "Limits update failed")) }
    }

    fun updateFilesQuery(q: String) { _state.update { it.copy(filesQuery = q) }; loadFiles(reset = true) }
    fun updateFilesPrivacy(p: AdminPrivacy?) { _state.update { it.copy(filesPrivacy = p) }; loadFiles(reset = true) }
    fun updateFileSort(sort: ContentSort) { _state.update { it.copy(fileSort = sort) }; loadFiles(reset = true) }
    fun loadMoreFiles() = loadFiles(reset = false)

    private fun loadFiles(reset: Boolean) = viewModelScope.launch {
        if (!_state.value.isAuthorized) return@launch
        val currentItems = (_state.value.filesState as? UiState.Success)?.value.orEmpty()
        val pageNum = if (reset) 1 else _state.value.filesNextPage ?: return@launch
        if (reset) _state.update { it.copy(filesState = UiState.Loading) }
        runCatching {
            repository.getFiles(
                FileQuery(
                    query = _state.value.filesQuery,
                    privacy = _state.value.filesPrivacy,
                    sort = _state.value.fileSort,
                    page = pageNum,
                    pageSize = 12
                )
            )
        }.onSuccess { page ->
            val merged = if (reset) page.items else currentItems + page.items
            _state.update {
                it.copy(
                    filesState = if (merged.isEmpty()) UiState.Empty else UiState.Success(merged),
                    filesNextPage = page.nextPage
                )
            }
        }.onFailure { fail("Failed to load files: ${it.message}") { s -> s.copy(filesState = UiState.Error("Failed to load files")) } }
    }

    fun toggleFileDownload(file: AdminFile, enabled: Boolean) = viewModelScope.launch {
        runCatching { repository.updateFile(file.id, UpdateAdminFileInput(downloadEnabled = enabled)) }
            .onSuccess {
                _events.send(AdminEvent.Snackbar("Download setting updated"))
                replaceFile(it)
                loadStats()
            }
            .onFailure { _events.send(AdminEvent.Snackbar(it.message ?: "Update failed")) }
    }

    fun changeFilePrivacy(file: AdminFile, privacy: AdminPrivacy) = viewModelScope.launch {
        runCatching { repository.updateFile(file.id, UpdateAdminFileInput(privacy = privacy)) }
            .onSuccess {
                _events.send(AdminEvent.Snackbar("Privacy updated"))
                replaceFile(it)
            }
            .onFailure { _events.send(AdminEvent.Snackbar(it.message ?: "Update failed")) }
    }

    fun revokeFileLink(file: AdminFile) = viewModelScope.launch {
        runCatching { repository.revokeFileLink(file.id) }
            .onSuccess {
                _events.send(AdminEvent.Snackbar("Link revoked"))
                replaceFile(it)
            }
            .onFailure { _events.send(AdminEvent.Snackbar(it.message ?: "Revoke failed")) }
    }

    fun deleteFile(file: AdminFile) = viewModelScope.launch {
        runCatching { repository.deleteFile(file.id) }
            .onSuccess {
                _events.send(AdminEvent.Snackbar("File deleted"))
                loadStats(); loadFiles(reset = true)
            }
            .onFailure { _events.send(AdminEvent.Snackbar(it.message ?: "Delete failed")) }
    }

    fun openFileDetail(file: AdminFile) { _state.update { it.copy(selectedFileDetail = file) } }
    fun closeFileDetail() { _state.update { it.copy(selectedFileDetail = null) } }
    fun openPublicPreview(file: AdminFile) { _state.update { it.copy(selectedPublicPreview = file) } }
    fun closePublicPreview() { _state.update { it.copy(selectedPublicPreview = null) } }

    fun updateLogFilters(eventType: AdminLogEventType?, actor: String, target: String) {
        _state.update { it.copy(logEventFilter = eventType, logActorFilter = actor, logTargetFilter = target) }
        loadLogs(reset = true)
    }
    fun loadMoreLogs() = loadLogs(reset = false)

    private fun loadLogs(reset: Boolean) = viewModelScope.launch {
        if (!_state.value.isAuthorized) return@launch
        val currentItems = (_state.value.logsState as? UiState.Success)?.value.orEmpty()
        val pageNum = if (reset) 1 else _state.value.logsNextPage ?: return@launch
        if (reset) _state.update { it.copy(logsState = UiState.Loading) }
        runCatching {
            repository.getLogs(
                LogQuery(
                    eventType = _state.value.logEventFilter,
                    actor = _state.value.logActorFilter,
                    target = _state.value.logTargetFilter,
                    page = pageNum,
                    pageSize = 20
                )
            )
        }.onSuccess { page ->
            val merged = if (reset) page.items else currentItems + page.items
            _state.update {
                it.copy(logsState = if (merged.isEmpty()) UiState.Empty else UiState.Success(merged), logsNextPage = page.nextPage)
            }
        }.onFailure { fail("Failed to load logs: ${it.message}") { s -> s.copy(logsState = UiState.Error("Failed to load logs")) } }
    }

    private fun loadStats() = viewModelScope.launch {
        if (!_state.value.isAuthorized) return@launch
        _state.update { it.copy(statsState = UiState.Loading) }
        runCatching { repository.getStats() }
            .onSuccess { _state.update { s -> s.copy(statsState = UiState.Success(it)) } }
            .onFailure { fail("Failed to load stats: ${it.message}") { s -> s.copy(statsState = UiState.Error("Failed to load stats")) } }
    }

    private fun loadSettings() = viewModelScope.launch {
        if (!_state.value.isAuthorized) return@launch
        _state.update { it.copy(settingsState = UiState.Loading) }
        runCatching { repository.getSettings() }
            .onSuccess { _state.update { s -> s.copy(settingsState = UiState.Success(it)) } }
            .onFailure { fail("Failed to load settings: ${it.message}") { s -> s.copy(settingsState = UiState.Error("Failed to load settings")) } }
    }

    fun saveSettings(settings: AdminSettings) = viewModelScope.launch {
        _state.update { it.copy(savingSettings = true) }
        runCatching { repository.updateSettings(settings) }
            .onSuccess {
                _state.update { s -> s.copy(settingsState = UiState.Success(it), savingSettings = false) }
                _events.send(AdminEvent.Snackbar("Settings saved"))
            }
            .onFailure {
                _state.update { s -> s.copy(savingSettings = false) }
                _events.send(AdminEvent.Snackbar("Save failed: ${it.message ?: "Unknown"}"))
            }
    }

    private fun replaceFile(file: AdminFile) {
        val current = (_state.value.filesState as? UiState.Success)?.value ?: return
        _state.update {
            it.copy(
                filesState = UiState.Success(current.map { existing -> if (existing.id == file.id) file else existing }),
                selectedFileDetail = it.selectedFileDetail?.takeIf { d -> d.id != file.id } ?: file
            )
        }
    }

    private fun fail(message: String, mutate: (AdminRouteState) -> AdminRouteState) {
        viewModelScope.launch {
            _events.send(AdminEvent.Snackbar(message))
            _state.update(mutate)
        }
    }
}
