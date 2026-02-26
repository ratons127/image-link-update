package com.qtiqo.share.feature.admin.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.qtiqo.share.feature.admin.domain.model.*
import com.qtiqo.share.util.formatBytes
import com.qtiqo.share.util.formatDateTime

@Composable
internal fun AddUserDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CreateAdminUserInput) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(AdminRole.USER) }
    var roleMenu by remember { mutableStateOf(false) }
    var storageLimit by remember { mutableStateOf("2147483648") }
    var maxUpload by remember { mutableStateOf("262144000") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") })
                OutlinedTextField(email, { email = it }, label = { Text("Email") })
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone (optional)") })
                OutlinedTextField(password, { password = it }, label = { Text("Password") })
                Box {
                    OutlinedButton(onClick = { roleMenu = true }) { Text("Role: ${role.name}") }
                    DropdownMenu(expanded = roleMenu, onDismissRequest = { roleMenu = false }) {
                        AdminRole.entries.forEach { r -> DropdownMenuItem(text = { Text(r.name) }, onClick = { role = r; roleMenu = false }) }
                    }
                }
                OutlinedTextField(storageLimit, { storageLimit = it }, label = { Text("Storage Limit (bytes)") })
                OutlinedTextField(maxUpload, { maxUpload = it }, label = { Text("Max Upload Size (bytes)") })
            }
        },
        confirmButton = {
            TextButton(enabled = !loading, onClick = {
                onSubmit(
                    CreateAdminUserInput(
                        name = name.trim(),
                        email = email.trim(),
                        phone = phone.ifBlank { null },
                        password = password,
                        role = role,
                        storageLimitBytes = storageLimit.toLongOrNull() ?: 0L,
                        maxUploadSizeBytes = maxUpload.toLongOrNull() ?: 0L
                    )
                )
            }) { Text(if (loading) "Creating..." else "Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun SetLimitsDialog(
    user: AdminUser,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit
) {
    var storage by remember { mutableStateOf(user.storageLimitBytes.toString()) }
    var upload by remember { mutableStateOf(user.maxUploadSizeBytes.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Limits") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(user.email)
                OutlinedTextField(storage, { storage = it }, label = { Text("Storage limit (bytes)") })
                OutlinedTextField(upload, { upload = it }, label = { Text("Max upload size (bytes)") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(storage.toLongOrNull() ?: user.storageLimitBytes, upload.toLongOrNull() ?: user.maxUploadSizeBytes) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun AdminFileDetailDialog(
    file: AdminFile,
    context: Context,
    onDismiss: () -> Unit,
    onOpenPublicView: () -> Unit,
    onToggleDownload: (Boolean) -> Unit,
    onChangePrivacy: (AdminPrivacy) -> Unit,
    onRevokeLink: () -> Unit
) {
    var privacyMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Detail") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AdminFilePreview(file)
                Text(file.filename, fontWeight = FontWeight.SemiBold)
                Text("Type: ${file.mimeType ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                Text("Size: ${formatBytes(file.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                Text("Owner: ${file.ownerEmail}", style = MaterialTheme.typography.bodySmall)
                Text("Uploaded: ${formatDateTime(file.createdAt)}", style = MaterialTheme.typography.bodySmall)
                Text("Share link: ${file.shareUrl ?: "Revoked / unavailable"}", maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = { file.shareUrl?.let { shareText(context, it) } }, enabled = file.shareUrl != null, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, null); Text(" Share")
                    }
                    FilledTonalButton(onClick = { file.shareUrl?.let { copyToClipboard(context, it) } }, enabled = file.shareUrl != null, modifier = Modifier.weight(1f)) {
                        Text("Copy link")
                    }
                }
                OutlinedButton(onClick = onOpenPublicView, enabled = file.shareUrl != null, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.OpenInNew, null)
                    Text(" Open Public View")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Allow downloads")
                    Switch(checked = file.downloadEnabled, onCheckedChange = onToggleDownload)
                }
                Box {
                    OutlinedButton(onClick = { privacyMenu = true }) { Text("Privacy: ${file.privacy.name}") }
                    DropdownMenu(expanded = privacyMenu, onDismissRequest = { privacyMenu = false }) {
                        AdminPrivacy.entries.forEach { privacy ->
                            DropdownMenuItem(text = { Text(privacy.name) }, onClick = { privacyMenu = false; onChangePrivacy(privacy) })
                        }
                    }
                }
                OutlinedButton(onClick = onRevokeLink, enabled = !file.isRevoked && file.shareUrl != null, modifier = Modifier.fillMaxWidth()) {
                    Text("Revoke link")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun AdminFilePreview(file: AdminFile) {
    val mime = file.mimeType.orEmpty()
    when {
        mime.startsWith("image/") -> AsyncImage(
            model = file.previewUrl ?: file.shareUrl,
            contentDescription = file.filename,
            modifier = Modifier.fillMaxWidth().height(180.dp).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        mime.startsWith("video/") -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val player = remember(file.id, file.previewUrl, file.shareUrl) {
                ExoPlayer.Builder(context).build().apply {
                    (file.previewUrl ?: file.shareUrl)?.let { setMediaItem(MediaItem.fromUri(it)) }
                    prepare()
                    playWhenReady = false
                }
            }
            DisposableEffect(player) { onDispose { player.release() } }
            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
        }
        else -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No inline preview", fontWeight = FontWeight.SemiBold)
                Text("Unknown/unsupported files show metadata only.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun AdminPublicViewDialog(
    file: AdminFile,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Public View (in-app)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminFilePreview(file)
                Text(file.filename, fontWeight = FontWeight.SemiBold)
                Text("Type: ${file.mimeType ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                Text("Size: ${formatBytes(file.sizeBytes)}", style = MaterialTheme.typography.bodySmall)
                if (file.downloadEnabled) {
                    Button(onClick = {}, enabled = true, modifier = Modifier.fillMaxWidth()) { Text("Download") }
                } else {
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Download Disabled") }
                }
                if (!(file.mimeType?.startsWith("image/") == true || file.mimeType?.startsWith("video/") == true)) {
                    Text("For safety, unknown mime types are not rendered raw.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun copyToClipboard(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("admin-share-link", value))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share link"))
}
