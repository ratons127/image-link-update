package com.qtiqo.share.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qtiqo.share.ui.viewmodel.AuthViewModel

@Composable
fun AuthLoginScreen(
    onGoSignUp: () -> Unit,
    onGoForgot: () -> Unit,
    onSignedIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var identifier by remember { mutableStateOf("demo@qtiqo.com") }
    var password by remember { mutableStateOf("demo123") }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.info) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
        if (state.info == "Signed in") onSignedIn()
    }

    AuthScaffold(snackbarHostState = snackbarHostState, title = "Qtiqo Share", subtitle = "Sign in to manage and share files") {
        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Email or Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.signIn(identifier, password) },
            enabled = !state.loading && identifier.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loading) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp))
            }
            Text("Sign In")
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onGoForgot) { Text("Forgot Password") }
            TextButton(onClick = onGoSignUp) { Text("Sign Up") }
        }
        Text(
            text = "Demo users: demo@qtiqo.com / demo123, admin / admin123",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignedUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.info) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
        if (state.info == "Account created") onSignedUp()
    }

    AuthScaffold(snackbarHostState, "Create account", "Auto-generated share links use imagelink.qtiqo.com") {
        OutlinedTextField(value = identifier, onValueChange = { identifier = it }, label = { Text("Email or Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { viewModel.signUp(identifier, password, phone) },
            enabled = !state.loading && identifier.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign Up") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Sign In") }
    }
}

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var identifier by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error, state.info) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
        state.info?.let { snackbarHostState.showSnackbar(it) }
    }

    AuthScaffold(snackbarHostState, "Forgot Password", "Submit your email or username") {
        OutlinedTextField(value = identifier, onValueChange = { identifier = it }, label = { Text("Email or Username") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.forgot(identifier) }, enabled = !state.loading && identifier.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Send Reset Link")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun AuthScaffold(
    snackbarHostState: SnackbarHostState,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Qtiqo Share", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        DemonBanner()
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun DemonBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(Color(0xFFFFF1EE), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(modifier = Modifier.size(58.dp)) {
            val red = Color(0xFFD92D20)
            val dark = Color(0xFF5C0F10)
            val cream = Color(0xFFFFF7E8)

            drawCircle(color = red, radius = size.minDimension * 0.42f, center = center)
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x - 18f, center.y - 20f)
                    lineTo(center.x - 10f, center.y - 34f)
                    lineTo(center.x - 2f, center.y - 18f)
                    close()
                },
                color = dark
            )
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x + 18f, center.y - 20f)
                    lineTo(center.x + 10f, center.y - 34f)
                    lineTo(center.x + 2f, center.y - 18f)
                    close()
                },
                color = dark
            )
            drawCircle(color = cream, radius = 5f, center = Offset(center.x - 10f, center.y - 4f))
            drawCircle(color = cream, radius = 5f, center = Offset(center.x + 10f, center.y - 4f))
            drawCircle(color = dark, radius = 2f, center = Offset(center.x - 10f, center.y - 4f))
            drawCircle(color = dark, radius = 2f, center = Offset(center.x + 10f, center.y - 4f))
            drawArc(
                color = dark,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - 14f, center.y + 4f),
                size = Size(28f, 18f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Demon mascot added", style = MaterialTheme.typography.titleMedium)
            Text("Visible on sign-in screens for branding flair.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
