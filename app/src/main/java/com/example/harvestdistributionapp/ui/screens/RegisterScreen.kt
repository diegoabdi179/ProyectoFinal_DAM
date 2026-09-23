package com.example.harvestdistributionapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
) {
    // 2. Conectar campos de texto y selector de rol al AuthViewModel
    val nombre by viewModel.nombre.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val rol by viewModel.rol.collectAsState()
    val authState by viewModel.authState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // 4. Observar el UiState: Muestra error en Snackbar y reinicia estado para evitar bloqueos
    LaunchedEffect(authState) {
        when (val state = authState) {
            is UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState() // Resetea a Idle permitiendo nuevos intentos ante errores
            }
            is UiState.Success -> {
                onNavigateToHome()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Regístrate en Cosecha Directa",
                style = MaterialTheme.typography.headlineSmall
            )

            // 3. Detectar estado de carga (Loading)
            val isLoading = authState is UiState.Loading

            // Campo Nombre completo
            OutlinedTextField(
                value = nombre,
                onValueChange = { viewModel.onNombreChanged(it) },
                label = { Text("Nombre completo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Campo Correo electrónico
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.onEmailChanged(it) },
                label = { Text("Correo electrónico") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            // Campo Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = { Text("Contraseña (mínimo 6 caracteres)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            // Selector de Rol ("Soy productor" o "Soy comprador")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Tipo de cuenta:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = rol == "comprador",
                        onClick = { viewModel.onRolChanged("comprador") },
                        label = { Text("Soy comprador") },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = rol == "productor",
                        onClick = { viewModel.onRolChanged("productor") },
                        label = { Text("Soy productor") },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 y 5. Botón "Crear cuenta": Deshabilitado en Loading, muestra CircularProgressIndicator y llama a registerUser()
            Button(
                onClick = { viewModel.registerUser() },
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
                    Text("Crear cuenta")
                }
            }

            TextButton(
                onClick = onNavigateToLogin,
                enabled = !isLoading
            ) {
                Text("¿Ya tienes una cuenta? Inicia sesión")
            }
        }
    }
}
