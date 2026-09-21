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
fun LoginScreen(navController: NavController, viewModel: AppViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var infoMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

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
                onValueChange = { email = it; errorMessage = null; infoMessage = null },
                type = "email",
                trailingIcon = Icons.Default.Mail,
                testTag = "login_email"
            )
            M3Field(
                label = "Contraseña",
                value = password,
                onValueChange = { password = it; errorMessage = null; infoMessage = null },
                type = "password",
                testTag = "login_password"
            )
            errorMessage?.let { ErrorBanner(it) }
            infoMessage?.let { InfoBanner(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextBtn(
                    "¿Olvidaste tu contraseña?",
                    onClick = { navController.navigate(Screen.ForgotPassword.route) },
                    testTag = "forgot_password"
                )
            }
            FilledBtn(
                text = if (submitting) "Iniciando…" else "Iniciar sesión",
                enabled = !submitting,
                testTag = "login_submit",
                onClick = {
                    errorMessage = InputValidator.validateEmail(email)
                        ?: if (password.isBlank()) "Ingresa tu contraseña" else null
                    if (errorMessage == null) {
                        submitting = true
                        viewModel.login(email, password) { result ->
                            submitting = false
                            when (result) {
                                is AppResult.Error -> errorMessage = result.message
                                is AppResult.Success -> navigateAfterAuthentication(navController, result.value)
                            }
                        }
                    }
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text("o", modifier = Modifier.padding(horizontal = 16.dp))
                HorizontalDivider(Modifier.weight(1f))
            }
            OutlinedBtn(
                text = "Continuar con Google",
                icon = Icons.Default.Language,
                testTag = "google_login",
                onClick = {
                    infoMessage = "Google Identity no está configurado en esta compilación. No se inició ninguna sesión."
                    errorMessage = null
                }
            )
            TextButton(
                onClick = { navController.navigate(Screen.SignUp.route) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("login_to_signup")
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
fun SignUpScreen(navController: NavController, viewModel: AppViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf<UserRole?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    Scaffold(topBar = { SmallTopBarM3("Crear cuenta", onBack = navController::popBackStack) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            M3Logo(size = 72.dp)
            Text("Únete a la comunidad", style = MaterialTheme.typography.headlineSmall)
            M3Field("Nombre completo", name, { name = it; errorMessage = null }, trailingIcon = Icons.Default.Person, testTag = "signup_name")
            M3Field("Correo electrónico", email, { email = it; errorMessage = null }, type = "email", trailingIcon = Icons.Default.Mail, testTag = "signup_email")
            M3Field("Contraseña", password, { password = it; errorMessage = null }, type = "password", supportingText = "Mínimo 8 caracteres, con letra y número", testTag = "signup_password")
            errorMessage?.let { ErrorBanner(it) }
            Text("¿Cómo quieres usar Cosecha Directa?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            RoleOption(
                "Soy productor",
                "Publica tu disponibilidad y atiende solicitudes.",
                Icons.Default.Agriculture,
                selectedRole == UserRole.PRODUCER,
                { selectedRole = UserRole.PRODUCER; errorMessage = null },
                "role_producer"
            )
            RoleOption(
                "Soy comprador",
                "Encuentra productos locales y solicita cantidades reales.",
                Icons.Default.LocalGroceryStore,
                selectedRole == UserRole.BUYER,
                { selectedRole = UserRole.BUYER; errorMessage = null },
                "role_buyer"
            )
            FilledBtn(
                text = if (submitting) "Creando…" else "Crear cuenta",
                enabled = !submitting,
                testTag = "signup_submit",
                onClick = {
                    errorMessage = when {
                        name.trim().length < 2 -> "Ingresa tu nombre completo"
                        InputValidator.validateEmail(email) != null -> InputValidator.validateEmail(email)
                        InputValidator.validatePassword(password) != null -> InputValidator.validatePassword(password)
                        selectedRole == null -> "Selecciona cómo usarás la aplicación"
                        else -> null
                    }
                    val role = selectedRole
                    if (errorMessage == null && role != null) {
                        submitting = true
                        viewModel.signUp(name, email, password, role) { result ->
                            submitting = false
                            when (result) {
                                is AppResult.Error -> errorMessage = result.message
                                is AppResult.Success -> navigateAfterAuthentication(navController, result.value)
                            }
                        }
                    }
                }
            )
            TextButton(onClick = { navController.navigate(Screen.Login.route) }, modifier = Modifier.heightIn(min = 48.dp)) {
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
