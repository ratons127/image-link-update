package com.qtiqo.share.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qtiqo.share.domain.model.FilePrivacy
import com.qtiqo.share.domain.model.UploadStatus
import com.qtiqo.share.ui.viewmodel.FileDetailViewModel
import com.qtiqo.share.util.formatBytes
import com.qtiqo.share.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    onBack: () -> Unit,
    viewModel: FileDetailViewModel = hiltViewModel()
) {
    val item by viewModel.file.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var privacyMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Detail") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { viewModel.delete(onBack) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        val file = item
        if (file == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("File not found") }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PreviewHeader(file.fileName, file.mimeType, file.localUri)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Share Link", style = MaterialTheme.typography.titleMedium)
                    Text(file.shareUrl ?: "Link not available yet", maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { file.shareUrl?.let { copyToClipboard(context, it) } },
                            enabled = file.shareUrl != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Copy") }
                        Button(
                            onClick = { file.shareUrl?.let { shareText(context, it) } },
                            enabled = file.shareUrl != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Share") }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Privacy & Downloads", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Privacy: ${file.privacy.name}")
                        Box {
                            TextButton(onClick = { privacyMenuExpanded = true }) { Text("Change") }
                            DropdownMenu(expanded = privacyMenuExpanded, onDismissRequest = { privacyMenuExpanded = false }) {
                                FilePrivacy.entries.forEach { privacy ->
                                    DropdownMenuItem(
                                        text = { Text(privacy.name) },
                                        onClick = {
                                            viewModel.setPrivacy(privacy)
                                            privacyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Allow Downloads")
                        Switch(checked = file.downloadEnabled, onCheckedChange = viewModel::toggleDownload)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Link Management", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { viewModel.revoke() },
                            enabled = file.shareUrl != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Revoke Link") }
                        Button(
                            onClick = { viewModel.regenerate() },
                            enabled = file.status == UploadStatus.DONE,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("  Regenerate Link")
                        }
                    }
                    if (file.revoked) Text("Current link has been revoked.", color = MaterialTheme.colorScheme.error)
                }
            }

            if (file.status == UploadStatus.FAILED || file.status == UploadStatus.CANCELED) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Upload ${file.status.name}")
                        Button(onClick = viewModel::retryUpload) { Text("Retry Upload") }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Details", style = MaterialTheme.typography.titleMedium)
                    DetailRow("Name", file.fileName)
                    DetailRow("Type", file.mimeType ?: "Unknown")
                    DetailRow("Size", formatBytes(file.sizeBytes))
                    DetailRow("Status", "${file.status.name} (${file.progress}%)")
                    DetailRow("Uploaded", file.uploadedAt?.let(::formatDateTime) ?: "-")
                    DetailRow("Created", formatDateTime(file.createdAt))
                }
            }
        }
    }
}

@Composable
private fun PreviewHeader(fileName: String, mimeType: String?, uri: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (mimeType?.startsWith("image/") == true) {
                AsyncImage(model = uri, contentDescription = fileName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FilePresent, null, modifier = Modifier.size(48.dp))
                    Text(fileName, modifier = Modifier.padding(top = 8.dp))
                    Text(mimeType ?: "Unknown type", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.padding(start = 12.dp))
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("share-link", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share link"))
}
