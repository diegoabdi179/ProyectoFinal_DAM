package com.example.harvestdistributionapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.harvestdistributionapp.AppUiState
import com.example.harvestdistributionapp.AppViewModel
import com.example.harvestdistributionapp.data.AppResult
import com.example.harvestdistributionapp.data.InputValidator
import com.example.harvestdistributionapp.data.UserAccount
import com.example.harvestdistributionapp.data.UserRole
import com.example.harvestdistributionapp.ui.components.*
import com.example.harvestdistributionapp.ui.navigation.Screen
import com.example.harvestdistributionapp.viewmodel.AuthViewModel
import com.example.harvestdistributionapp.viewmodel.UiState
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, uiState: AppUiState) {
    var navigated by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isReady, uiState.appState.currentUserId) {
        if (uiState.isReady && !navigated) {
            delay(900)
            val destination = when (uiState.appState.currentUser?.role) {
                UserRole.PRODUCER -> Screen.ProducerHome.route
                UserRole.BUYER -> Screen.BuyerHome.route
                null -> Screen.Welcome.route
            }
            navigated = true
            navController.navigate(destination) { popUpTo(Screen.Splash.route) { inclusive = true } }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            M3Logo(size = 96.dp, color = Color.White)
            Spacer(Modifier.height(24.dp))
            Text("Cosecha Directa", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            if (!uiState.isReady) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
fun WelcomeScreen(navController: NavController) {
    Scaffold(
        topBar = { SmallTopBarM3(title = "Cosecha Directa") },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800&h=600&fit=crop&auto=format",
                contentDescription = "Campo agrícola al amanecer",
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                "Productos locales.\nConexiones directas.",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, lineHeight = 40.sp),
                fontWeight = FontWeight.Medium
            )
            Text(
                "Encuentra lo que necesitas o conecta tu producción con nuevos compradores.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledBtn(
                text = "Comenzar",
                onClick = { navController.navigate(Screen.SignUp.route) },
                testTag = "welcome_start"
            )
            TextButton(
                onClick = { navController.navigate(Screen.Login.route) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("welcome_login")
            ) {
                Text("¿Ya tienes cuenta? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Iniciar sesión", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val authState by viewModel.authState.collectAsState()

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is UiState.Error -> {
                errorMessage = state.message
                viewModel.resetState()
            }
            is UiState.Success -> {
                val uid = state.data.uid
                try {
                    val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(uid)
                        .get()
                        .await()
                    val rolDb = doc.getString("rol") ?: "comprador"
                    val destination = if (rolDb.equals("productor", ignoreCase = true)) {
                        Screen.ProducerHome.route
                    } else {
                        Screen.BuyerHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } catch (_: Exception) {
                    navController.navigate(Screen.BuyerHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            else -> {}
        }
    }

    val submitting = authState is UiState.Loading

    Scaffold(topBar = { SmallTopBarM3("Bienvenido", onBack = navController::popBackStack) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3Logo(size = 80.dp)
            Text("Inicia sesión para continuar", style = MaterialTheme.typography.headlineSmall)
            M3Field(
                label = "Correo electrónico",
                value = email,
                onValueChange = { viewModel.onEmailChanged(it); errorMessage = null },
                type = "email",
                trailingIcon = Icons.Default.Mail,
                testTag = "login_email",
                enabled = !submitting
            )
            M3Field(
                label = "Contraseña",
                value = password,
                onValueChange = { viewModel.onPasswordChanged(it); errorMessage = null },
                type = "password",
                testTag = "login_password",
                enabled = !submitting
            )
            errorMessage?.let { ErrorBanner(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextBtn(
                    "¿Olvidaste tu contraseña?",
                    onClick = { navController.navigate(Screen.ForgotPassword.route) },
                    testTag = "forgot_password"
                )
            }
            Button(
                onClick = { viewModel.loginUser() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("login_submit"),
                enabled = !submitting,
                shape = CircleShape
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Iniciar sesión", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(
                onClick = { navController.navigate(Screen.SignUp.route) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("login_to_signup"),
                enabled = !submitting
            ) {
                Text("¿No tienes cuenta? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Crear cuenta", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { SmallTopBarM3("Recuperar contraseña", onBack = navController::popBackStack) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Recuperación de cuenta", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Esta versión local no dispone de un servidor de correo. La acción informa el estado real y no simula el envío.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            M3Field(
                label = "Correo electrónico",
                value = email,
                onValueChange = { email = it; error = null; message = null },
                type = "email",
                testTag = "recovery_email"
            )
            error?.let { ErrorBanner(it) }
            message?.let { InfoBanner(it) }
            FilledBtn(
                "Solicitar recuperación",
                testTag = "recovery_submit",
                onClick = {
                    error = InputValidator.validateEmail(email)
                    if (error == null) {
                        message = "No se envió ningún correo: configura un proveedor de identidad o backend antes de habilitar recuperación remota."
                    }
                }
            )
        }
    }
}

@Composable
fun SignUpScreen(navController: NavController, viewModel: AuthViewModel) {
    val name by viewModel.nombre.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val rol by viewModel.rol.collectAsState()
    val authState by viewModel.authState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            is UiState.Success -> {
                val destination = if (state.rol.trim().equals("productor", ignoreCase = true)) {
                    Screen.ProducerHome.route
                } else {
                    Screen.BuyerHome.route
                }
                navController.navigate(destination) {
                    popUpTo(Screen.SignUp.route) { inclusive = true }
                    popUpTo(Screen.Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            else -> {}
        }
    }

    val submitting = authState is UiState.Loading

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { SmallTopBarM3("Crear cuenta", onBack = navController::popBackStack) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3Logo(size = 72.dp)
            Text("Únete a la comunidad", style = MaterialTheme.typography.headlineSmall)
            M3Field(
                label = "Nombre completo",
                value = name,
                onValueChange = { viewModel.onNombreChanged(it) },
                trailingIcon = Icons.Default.Person,
                testTag = "signup_name",
                enabled = !submitting
            )
            M3Field(
                label = "Correo electrónico",
                value = email,
                onValueChange = { viewModel.onEmailChanged(it) },
                type = "email",
                trailingIcon = Icons.Default.Mail,
                testTag = "signup_email",
                enabled = !submitting
            )
            M3Field(
                label = "Contraseña",
                value = password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                type = "password",
                supportingText = "Mínimo 6 caracteres",
                testTag = "signup_password",
                enabled = !submitting
            )
            Text("¿Cómo quieres usar Cosecha Directa?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            RoleOption(
                "Soy productor",
                "Publica tu disponibilidad y atiende solicitudes.",
                Icons.Default.Agriculture,
                rol == "productor",
                { viewModel.onRolChanged("productor") },
                "role_producer"
            )
            RoleOption(
                "Soy comprador",
                "Encuentra productos locales y solicita cantidades reales.",
                Icons.Default.LocalGroceryStore,
                rol == "comprador",
                { viewModel.onRolChanged("comprador") },
                "role_buyer"
            )
            Button(
                onClick = { viewModel.registerUser() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("signup_submit"),
                enabled = !submitting,
                shape = CircleShape
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear cuenta", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(
                onClick = { navController.navigate(Screen.Login.route) },
                modifier = Modifier.heightIn(min = 48.dp),
                enabled = !submitting
            ) {
                Text("¿Ya tienes cuenta? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Iniciar sesión", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun RoleOption(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Default.CheckCircle, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun navigateAfterAuthentication(navController: NavController, user: UserAccount) {
    val destination = if (user.role == UserRole.PRODUCER) Screen.ProducerHome.route else Screen.BuyerHome.route
    navController.navigate(destination) {
        popUpTo(Screen.Welcome.route) { inclusive = true }
        launchSingleTop = true
    }
}
