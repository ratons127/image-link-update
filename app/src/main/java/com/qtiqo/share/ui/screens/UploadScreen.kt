package com.qtiqo.share.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtiqo.share.domain.model.UploadStatus
import com.qtiqo.share.ui.viewmodel.UploadViewModel

@Composable
fun UploadScreen(
    onOpenDetail: (String) -> Unit,
    viewModel: UploadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.onFilePicked(context, it) }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Upload File", style = MaterialTheme.typography.headlineSmall)
        DashedChooseFileBox(
            onClick = { picker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth().height(180.dp)
        )
        Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileUpload, contentDescription = null)
            Text("  Choose a File")
        }
        Text("Recent uploads", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDetail(item.id) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.fileName)
                        Text(item.mimeType ?: "Unknown type", style = MaterialTheme.typography.bodySmall)
                        if (item.status == UploadStatus.UPLOADING || item.status == UploadStatus.QUEUED) {
                            LinearProgressIndicator(progress = { item.progress / 100f }, modifier = Modifier.fillMaxWidth())
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.status.name} ${item.progress}%")
                                TextButton(onClick = { viewModel.cancelUpload(item.id) }) { Text("Cancel") }
                            }
                        } else if (item.status == UploadStatus.FAILED || item.status == UploadStatus.CANCELED) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(item.status.name, color = MaterialTheme.colorScheme.error)
                                Row {
                                    IconButton(onClick = { viewModel.retryUpload(item.id) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                                    }
                                    IconButton(onClick = { viewModel.deleteUpload(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                                    }
                                }
                            }
                        } else {
                            Text(if (item.shareUrl != null) "Link ready" else item.status.name)
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbar)
    }
}

@Composable
private fun DashedChooseFileBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize().padding(8.dp)) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(20f, 12f), 0f)
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(28f, 28f),
                style = Stroke(width = 4f, pathEffect = dash)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(42.dp))
            Text("Choose a File", style = MaterialTheme.typography.titleLarge)
            Text("Images, docs, videos, and more", style = MaterialTheme.typography.bodySmall)
        }
    }
}
