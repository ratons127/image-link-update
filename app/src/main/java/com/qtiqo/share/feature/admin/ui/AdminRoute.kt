package com.qtiqo.share.feature.admin.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtiqo.share.feature.admin.ui.vm.AdminEvent
import com.qtiqo.share.feature.admin.ui.vm.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRoute(
    onBack: () -> Unit,
    onOpenShareLink: (String) -> Unit,
    viewModel: AdminViewModel
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val snack = remember { SnackbarHostState() }
    val showAddUser = remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            if (it is AdminEvent.Snackbar) snack.showSnackbar(it.message)
        }
    }

    if (!state.isAuthorized) {
        NotAuthorizedScreen(onBack)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    Button(onClick = { showAddUser.value = true }) {
                        Icon(Icons.Default.Add, null)
                        Text(" Add User")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        AdminPanelScaffold(
            state = state,
            viewModel = viewModel,
            onOpenShareLink = onOpenShareLink,
            context = context,
            modifier = Modifier.padding(padding)
        )
    }

    if (showAddUser.value) {
        AddUserDialog(
            loading = state.addingUser,
            onDismiss = { showAddUser.value = false },
            onSubmit = {
                viewModel.addUser(it)
                showAddUser.value = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotAuthorizedScreen(onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Admin") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Card {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Not authorized")
                    Text("Admin access is required.")
                }
            }
        }
    }
}
