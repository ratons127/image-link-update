package com.qtiqo.share.feature.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtiqo.share.feature.profile.domain.models.ProfileSummary
import com.qtiqo.share.feature.profile.vm.ProfileEvent
import com.qtiqo.share.feature.profile.vm.ProfileScreenState
import com.qtiqo.share.feature.profile.vm.ProfileSummaryUiState
import com.qtiqo.share.feature.profile.vm.ProfileViewModel
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
                ProfileEvent.NavigateToLogin -> onLoggedOut()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ProfileContent(
            state = state,
            onCurrentPasswordChange = viewModel::onCurrentPasswordChanged,
            onNewPasswordChange = viewModel::onNewPasswordChanged,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
            onUpdatePassword = viewModel::updatePassword,
            onSignOut = viewModel::signOut,
            onRetrySummary = viewModel::loadSummary,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun ProfileContent(
    state: ProfileScreenState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onUpdatePassword: () -> Unit,
    onSignOut: () -> Unit,
    onRetrySummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = (state.summaryState as? ProfileSummaryUiState.Success)?.summary
    val usedBytes = summary?.storageUsedBytes ?: 0L
    val limitBytes = summary?.storageLimitBytes ?: 1L
    val usagePct = ((usedBytes.toDouble() / limitBytes.toDouble()) * 100.0).toInt().coerceIn(0, 100)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        state.sessionEmail?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when (val summaryState = state.summaryState) {
            ProfileSummaryUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            is ProfileSummaryUiState.Error -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(summaryState.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetrySummary) { Text("Retry") }
                    }
                }
            }
            is ProfileSummaryUiState.Success -> {
                SummaryStatsRow(summaryState.summary)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Storage Usage", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("$usagePct%", fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(
                    progress = { (usedBytes.toFloat() / limitBytes.coerceAtLeast(1L)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${formatStorage(usedBytes)} / ${formatStorage(limitBytes)} used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Change Password", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.passwordForm.currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = { Text("Current password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    isError = state.passwordForm.currentError != null,
                    supportingText = { state.passwordForm.currentError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.passwordForm.newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("New password") },
                    leadingIcon = { Icon(Icons.Default.Password, null) },
                    isError = state.passwordForm.newError != null,
                    supportingText = { state.passwordForm.newError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.passwordForm.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm new password") },
                    leadingIcon = { Icon(Icons.Default.Password, null) },
                    isError = state.passwordForm.confirmError != null,
                    supportingText = { state.passwordForm.confirmError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onUpdatePassword,
                    enabled = state.passwordForm.isValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Security, null)
                    if (state.passwordForm.isSubmitting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp).padding(start = 8.dp)
                        )
                    }
                    Text(" Update Password")
                }
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            enabled = !state.signingOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
            )
        ) {
            if (state.signingOut) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            }
            Text("Sign Out")
        }
    }
}

@Composable
fun SummaryStatsRow(summary: ProfileSummary, filesLabel: String = "Files") {
    BoxWithConstraints {
        if (maxWidth < 380.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(title = filesLabel, value = summary.filesCount.toString(), modifier = Modifier.width(120.dp))
                StatCard(title = "Storage", value = formatStorage(summary.storageUsedBytes), modifier = Modifier.width(140.dp))
                StatCard(title = "Upload Limit", value = formatStorage(summary.uploadLimitBytes), modifier = Modifier.width(150.dp))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(title = filesLabel, value = summary.filesCount.toString(), modifier = Modifier.weight(1f))
                StatCard(title = "Storage", value = formatStorage(summary.storageUsedBytes), modifier = Modifier.weight(1f))
                StatCard(title = "Upload Limit", value = formatStorage(summary.uploadLimitBytes), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

private fun formatStorage(bytes: Long): String {
    if (bytes < 1024L * 1024L * 1024L) {
        return "${oneDecimal(bytes / (1024.0 * 1024.0))} MB"
    }
    return "${oneDecimal(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

private fun oneDecimal(value: Double): String {
    val rounded = round(value * 10.0) / 10.0
    return String.format("%.1f", rounded)
}
