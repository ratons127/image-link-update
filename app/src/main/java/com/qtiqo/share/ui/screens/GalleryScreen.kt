package com.qtiqo.share.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qtiqo.share.data.local.entity.UploadEntity
import com.qtiqo.share.domain.model.UploadStatus
import com.qtiqo.share.ui.viewmodel.GalleryFilter
import com.qtiqo.share.ui.viewmodel.GallerySort
import com.qtiqo.share.ui.viewmodel.GalleryViewModel
import com.qtiqo.share.util.formatBytes
import com.qtiqo.share.util.formatDateTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onUploadClick: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sortExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GalleryHeaderDemon()
        OutlinedTextField(
            value = state.search,
            onValueChange = viewModel::setSearch,
            label = { Text("Search files") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "All" to GalleryFilter.ALL,
                "Public" to GalleryFilter.PUBLIC,
                "Unlisted" to GalleryFilter.UNLISTED,
                "Private" to GalleryFilter.PRIVATE
            ).forEach { (label, value) ->
                AssistChip(onClick = { viewModel.setFilter(value) }, label = { Text(label) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Sort: ${state.sort.name}", maxLines = 1)
            Box {
                TextButton(onClick = { sortExpanded = true }) { Text("Sort") }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    GallerySort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                viewModel.setSort(option)
                                sortExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (state.items.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth().weight(1f), tonalElevation = 2.dp, shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No files yet", style = MaterialTheme.typography.titleLarge)
                    Text("Upload a file to generate a public link.", modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
                    androidx.compose.material3.Button(onClick = onUploadClick) { Text("Upload File") }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 145.dp),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    UploadGridCard(
                        item = item,
                        onClick = { onOpenDetail(item.id) },
                        onCancel = { viewModel.cancelUpload(item.id) },
                        onRetry = { viewModel.retryUpload(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryHeaderDemon() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3EE), MaterialTheme.shapes.large)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(42.dp)) {
            val red = Color(0xFFD92D20)
            val dark = Color(0xFF5C0F10)
            val cream = Color(0xFFFFF7E8)
            drawCircle(color = red, radius = size.minDimension * 0.42f, center = center)
            drawPath(
                path = Path().apply {
                    moveTo(center.x - 12f, center.y - 12f)
                    lineTo(center.x - 6f, center.y - 22f)
                    lineTo(center.x - 2f, center.y - 10f)
                    close()
                },
                color = dark
            )
            drawPath(
                path = Path().apply {
                    moveTo(center.x + 12f, center.y - 12f)
                    lineTo(center.x + 6f, center.y - 22f)
                    lineTo(center.x + 2f, center.y - 10f)
                    close()
                },
                color = dark
            )
            drawCircle(color = cream, radius = 3.6f, center = Offset(center.x - 7f, center.y - 2f))
            drawCircle(color = cream, radius = 3.6f, center = Offset(center.x + 7f, center.y - 2f))
            drawCircle(color = dark, radius = 1.5f, center = Offset(center.x - 7f, center.y - 2f))
            drawCircle(color = dark, radius = 1.5f, center = Offset(center.x + 7f, center.y - 2f))
            drawArc(
                color = dark,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - 10f, center.y + 3f),
                size = Size(20f, 12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
        Column {
            Text("Qtiqo Share", style = MaterialTheme.typography.headlineSmall)
            Text("Demon edition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UploadGridCard(
    item: UploadEntity,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(104.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.mimeType?.startsWith("image/") == true) {
                    AsyncImage(
                        model = item.localUri,
                        contentDescription = item.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.FilePresent, contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }
            Text(item.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatBytes(item.sizeBytes), style = MaterialTheme.typography.bodySmall)
            Text(formatDateTime(item.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when (item.status) {
                UploadStatus.UPLOADING, UploadStatus.QUEUED -> {
                    LinearProgressIndicator(progress = { item.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.status.name} ${item.progress}%")
                        TextButton(onClick = onCancel) { Text("Cancel") }
                    }
                }
                UploadStatus.FAILED, UploadStatus.CANCELED -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.status.name, color = MaterialTheme.colorScheme.error)
                        IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, contentDescription = "Retry") }
                    }
                }
                UploadStatus.DONE -> {
                    Text(
                        if (item.revoked) "Link revoked" else "Public link ready",
                        color = if (item.revoked) MaterialTheme.colorScheme.error else Color(0xFF1E8E3E),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
