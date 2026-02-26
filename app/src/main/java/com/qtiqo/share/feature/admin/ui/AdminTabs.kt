package com.qtiqo.share.feature.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import com.qtiqo.share.feature.admin.domain.model.*
import com.qtiqo.share.feature.admin.ui.vm.*
import com.qtiqo.share.util.formatBytes
import com.qtiqo.share.util.formatDateTime

@Composable
internal fun AdminPanelScaffold(
    state: AdminRouteState,
    viewModel: AdminViewModel,
    onOpenShareLink: (String) -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatsCards(state.statsState)
        TabRow(selectedTabIndex = state.selectedTab.ordinal) {
            AdminPanelTab.entries.forEach { tab ->
                Tab(selected = tab == state.selectedTab, onClick = { viewModel.selectTab(tab) }, text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        when (state.selectedTab) {
            AdminPanelTab.USERS -> UsersTabContent(state, viewModel)
            AdminPanelTab.CONTENT -> ContentTabContent(state, viewModel)
            AdminPanelTab.LOGS -> LogsTabContent(state, viewModel)
            AdminPanelTab.SETTINGS -> SettingsTabContent(state, viewModel)
        }
    }

    state.selectedFileDetail?.let { file ->
        AdminFileDetailDialog(
            file = file,
            context = context,
            onDismiss = viewModel::closeFileDetail,
            onOpenPublicView = {
                file.shareUrl?.let(onOpenShareLink)
                viewModel.openPublicPreview(file)
            },
            onToggleDownload = { viewModel.toggleFileDownload(file, it) },
            onChangePrivacy = { viewModel.changeFilePrivacy(file, it) },
            onRevokeLink = { viewModel.revokeFileLink(file) }
        )
    }
    state.selectedPublicPreview?.let { AdminPublicViewDialog(it, viewModel::closePublicPreview) }
}

@Composable
private fun StatsCards(state: UiState<AdminStats>) {
    when (state) {
        UiState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
        UiState.Empty -> Text("No stats")
        is UiState.Success -> {
            val cards = listOf(
                "Users" to state.value.totalUsers.toString(),
                "Files" to state.value.totalFiles.toString(),
                "Storage" to formatBytes(state.value.totalStorageUsedBytes),
                "Views" to state.value.totalViews.toString()
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { (label, value) ->
                            Card(Modifier.weight(1f)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(label, style = MaterialTheme.typography.bodySmall)
                                    Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersTabContent(state: AdminRouteState, viewModel: AdminViewModel) {
    var limitDialogFor by remember { mutableStateOf<AdminUser?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(state.usersQuery, viewModel::updateUsersQuery, label = { Text("Search users") }, modifier = Modifier.fillMaxWidth())
        StateList(state.usersState, "No users") { users ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        isCurrent = state.session?.email.equals(user.email, true),
                        onToggleRole = { viewModel.toggleUserRole(user) },
                        onToggleSuspend = { viewModel.toggleUserSuspended(user) },
                        onDelete = { viewModel.deleteUser(user) },
                        onSetLimits = { limitDialogFor = user }
                    )
                }
                item { if (state.usersNextPage != null) TextButton(onClick = viewModel::loadMoreUsers, modifier = Modifier.fillMaxWidth()) { Text("Load more") } }
            }
        }
    }
    limitDialogFor?.let { user ->
        SetLimitsDialog(user, onDismiss = { limitDialogFor = null }) { storage, upload ->
            viewModel.setUserLimits(user, storage, upload)
            limitDialogFor = null
        }
    }
}

@Composable
private fun UserCard(
    user: AdminUser,
    isCurrent: Boolean,
    onToggleRole: () -> Unit,
    onToggleSuspend: () -> Unit,
    onDelete: () -> Unit,
    onSetLimits: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(user.name.firstOrNull()?.uppercase() ?: "U", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.name, fontWeight = FontWeight.SemiBold)
                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(if (user.role == AdminRole.ADMIN) "Demote" else "Promote") }, onClick = { menu = false; onToggleRole() })
                        DropdownMenuItem(text = { Text(if (user.isSuspended) "Activate" else "Suspend") }, onClick = { menu = false; onToggleSuspend() })
                        DropdownMenuItem(text = { Text("Set Limits") }, onClick = { menu = false; onSetLimits() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(onClick = {}, label = { Text(user.role.name) })
                AssistChip(onClick = {}, label = { Text(user.plan.name) })
                if (user.isSuspended) {
                    AssistChip(onClick = {}, label = { Text("SUSPENDED") })
                }
            }
            Text("${formatBytes(user.storageUsedBytes)} / ${formatBytes(user.storageLimitBytes)}", style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { (user.storageUsedBytes.toFloat() / user.storageLimitBytes.coerceAtLeast(1L)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${user.fileCount} files", style = MaterialTheme.typography.bodySmall)
                if (isCurrent) Text("You", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ContentTabContent(state: AdminRouteState, viewModel: AdminViewModel) {
    var privacyMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(state.filesQuery, viewModel::updateFilesQuery, label = { Text("Search filename") }, modifier = Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            Box {
                OutlinedButton(onClick = { privacyMenu = true }) { Text("Privacy: ${state.filesPrivacy?.name ?: "ALL"}") }
                DropdownMenu(expanded = privacyMenu, onDismissRequest = { privacyMenu = false }) {
                    DropdownMenuItem(text = { Text("ALL") }, onClick = { privacyMenu = false; viewModel.updateFilesPrivacy(null) })
                    AdminPrivacy.entries.forEach { p -> DropdownMenuItem(text = { Text(p.name) }, onClick = { privacyMenu = false; viewModel.updateFilesPrivacy(p) }) }
                }
            }
            Box {
                OutlinedButton(onClick = { sortMenu = true }) { Text("Sort: ${state.fileSort.name}") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    ContentSort.entries.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { sortMenu = false; viewModel.updateFileSort(s) }) }
                }
            }
        }
        StateList(state.filesState, "No files") { files ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files, key = { it.id }) { file ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(file.filename, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(file.ownerEmail, style = MaterialTheme.typography.bodySmall)
                                }
                                AssistChip(onClick = {}, label = { Text(file.privacy.name) })
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatBytes(file.sizeBytes), style = MaterialTheme.typography.bodySmall)
                                Text("${file.views} views", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Download")
                                    Switch(checked = file.downloadEnabled, onCheckedChange = { viewModel.toggleFileDownload(file, it) })
                                }
                                Row {
                                    TextButton(onClick = { viewModel.openFileDetail(file) }) { Text("Detail") }
                                    IconButton(onClick = { viewModel.deleteFile(file) }) { Icon(Icons.Default.Delete, null) }
                                }
                            }
                        }
                    }
                }
                item { if (state.filesNextPage != null) TextButton(onClick = viewModel::loadMoreFiles, modifier = Modifier.fillMaxWidth()) { Text("Load more") } }
            }
        }
    }
}

@Composable
private fun LogsTabContent(state: AdminRouteState, viewModel: AdminViewModel) {
    var eventFilter by remember(state.logEventFilter) { mutableStateOf(state.logEventFilter) }
    var actor by remember(state.logActorFilter) { mutableStateOf(state.logActorFilter) }
    var target by remember(state.logTargetFilter) { mutableStateOf(state.logTargetFilter) }
    var menu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        Box {
            OutlinedButton(onClick = { menu = true }) { Text(eventFilter?.name ?: "All events") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("All") }, onClick = { eventFilter = null; menu = false })
                AdminLogEventType.entries.forEach { event ->
                    DropdownMenuItem(text = { Text(event.name) }, onClick = { eventFilter = event; menu = false })
                }
            }
        }
        OutlinedTextField(actor, { actor = it }, label = { Text("Actor email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(target, { target = it }, label = { Text("Target id") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.updateLogFilters(eventFilter, actor, target) }, modifier = Modifier.fillMaxWidth()) { Text("Apply Filters") }
        StateList(state.logsState, "No logs") { logs ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs, key = { it.id }) { log ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${log.eventType.name} • ${formatDateTime(log.createdAt)}", fontWeight = FontWeight.SemiBold)
                            Text("Actor: ${log.actorEmail}", style = MaterialTheme.typography.bodySmall)
                            Text(log.message)
                            Text("Target: ${log.targetType.name}/${log.targetId}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item { if (state.logsNextPage != null) TextButton(onClick = viewModel::loadMoreLogs, modifier = Modifier.fillMaxWidth()) { Text("Load more") } }
            }
        }
    }
}

@Composable
private fun SettingsTabContent(state: AdminRouteState, viewModel: AdminViewModel) {
    val settings = (state.settingsState as? UiState.Success)?.value
    when (state.settingsState) {
        UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((state.settingsState as UiState.Error).message) }
        UiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No settings") }
        is UiState.Success -> {
            var form by remember(settings) { mutableStateOf(settings!!) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                NumberField("Default storage limit (bytes)", form.defaultStorageLimitBytes) { form = form.copy(defaultStorageLimitBytes = it) }
                NumberField("Max upload size (bytes)", form.maxUploadSizeBytes) { form = form.copy(maxUploadSizeBytes = it) }
                NumberField("Rate limit per minute", form.rateLimitPerMinute.toLong()) { form = form.copy(rateLimitPerMinute = it.toInt()) }
                LabeledSwitch("Downloads enabled by default", form.downloadsEnabledByDefault) { form = form.copy(downloadsEnabledByDefault = it) }
                LabeledSwitch("Public pages enabled", form.publicPagesEnabled) { form = form.copy(publicPagesEnabled = it) }
                LabeledSwitch("CAPTCHA enabled", form.captchaEnabled) { form = form.copy(captchaEnabled = it) }
                Button(onClick = { viewModel.saveSettings(form) }, enabled = !state.savingSettings, modifier = Modifier.fillMaxWidth()) { Text(if (state.savingSettings) "Saving..." else "Save") }
            }
        }
    }
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun NumberField(label: String, value: Long, onChange: (Long) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toLongOrNull() ?: value) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun <T> StateList(state: UiState<List<T>>, emptyText: String, content: @Composable (List<T>) -> Unit) {
    when (state) {
        UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
        UiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emptyText) }
        is UiState.Success -> content(state.value)
    }
}
