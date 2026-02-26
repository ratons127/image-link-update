package com.qtiqo.share.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.qtiqo.share.ui.viewmodel.PublicViewModel
import com.qtiqo.share.util.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicViewScreen(
    onBack: () -> Unit,
    onOpenInAppGallery: () -> Unit,
    viewModel: PublicViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Public View") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.error ?: "Unable to open", color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::load) { Text("Retry") }
                    OutlinedButton(onClick = onOpenInAppGallery) { Text("Open App") }
                }
            }
            else -> {
                val item = state.item ?: return@Scaffold
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (item.isImage && item.localUri != null) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = item.localUri,
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxWidth().height(260.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.FilePresent, null, modifier = Modifier.size(48.dp))
                                    Text(item.name, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text("Type: ${item.mimeType ?: "Unknown"}")
                            Text("Size: ${formatBytes(item.sizeBytes)}")
                            Text("Link: https://imagelink.qtiqo.com/s/${item.shareToken}")
                        }
                    }

                    if (item.allowDownloads) {
                        Button(
                            onClick = { item.localUri?.let { openFile(context, it, item.mimeType) } },
                            enabled = item.localUri != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Download / Open File") }
                    } else {
                        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text("Downloads Disabled")
                        }
                    }
                }
            }
        }
    }
}

private fun openFile(context: Context, uriString: String, mimeType: String?) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(uriString), mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open file"))
}
