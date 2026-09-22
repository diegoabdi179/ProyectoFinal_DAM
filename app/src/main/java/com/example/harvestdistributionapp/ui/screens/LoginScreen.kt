package com.example.harvestdistributionapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.harvestdistributionapp.viewmodel.AuthViewModel
import com.example.harvestdistributionapp.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
) {
    // Solicitar permiso de notificaciones en Android 13+ al abrir la pantalla
    com.example.harvestdistributionapp.ui.components.NotificationPermissionHandler()

    // 2. Observar el UiState y los campos usando collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val authState by viewModel.authState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // 4. Si el estado es UiState.Error, mostrar mensaje en el Snackbar usando LaunchedEffect
    // 5. Si el estado es UiState.Success, ejecutar onNavigateToHome()
    LaunchedEffect(authState) {
        when (val state = authState) {
            is UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            is UiState.Success -> {
                onNavigateToHome()
            }
            else -> {}
        }
    }

    // 1. Scaffold que contiene un SnackbarHost
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Iniciar Sesión") },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido a Cosecha Directa",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 6. Componente M3 OutlinedTextField para email
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.onEmailChanged(it) },
                label = { Text("Correo electrónico") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                enabled = authState !is UiState.Loading
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 6. Componente M3 OutlinedTextField para password
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                enabled = authState !is UiState.Loading
            )
            Spacer(modifier = Modifier.height(24.dp))

            val isLoading = authState is UiState.Loading

            // 3. Si el estado es UiState.Loading, deshabilita el botón y muestra CircularProgressIndicator
            // 6. Componente M3 Button
            Button(
                onClick = { viewModel.loginUser() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Iniciar sesión")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToRegister,
                enabled = !isLoading
            ) {
                Text("¿No tienes una cuenta? Regístrate")
            }
        }
    }
}
