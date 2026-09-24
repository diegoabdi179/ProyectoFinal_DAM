package com.example.harvestdistributionapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.harvestdistributionapp.AppViewModel
import com.example.harvestdistributionapp.viewmodel.AuthViewModel
import kotlinx.coroutines.tasks.await
import com.example.harvestdistributionapp.BuildConfig
import com.example.harvestdistributionapp.data.*
import com.example.harvestdistributionapp.ui.components.*
import com.example.harvestdistributionapp.ui.navigation.Screen
import com.example.harvestdistributionapp.ui.theme.SurfaceContainerHigh

@Composable
fun ProfileScreen(navController: NavController, state: AppState, viewModel: AppViewModel, authViewModel: AuthViewModel) {
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    var firestoreRole by remember { mutableStateOf("") }
    var firestoreName by remember { mutableStateOf(firebaseUser?.displayName ?: "Usuario") }
    var firestoreLocation by remember { mutableStateOf("Toluca, Estado de México") }
    var firestoreBusiness by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(firebaseUser?.uid) {
        val uid = firebaseUser?.uid
        if (uid != null) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .get()
                    .await()
                if (doc.exists()) {
                    firestoreRole = doc.getString("rol") ?: ""
                    firestoreName = doc.getString("nombre") ?: firebaseUser.displayName ?: "Usuario"
                    firestoreLocation = doc.getString("ubicacion") ?: "Toluca, Estado de México"
                    firestoreBusiness = doc.getString("negocio").orEmpty()
                }
            } catch (_: Exception) {}
        }
        isLoading = false
    }

    val user = state.currentUser
    val role = if (user != null) {
        if (user.role == UserRole.PRODUCER) "productor" else "comprador"
    } else {
        firestoreRole
    }

    val isProducer = role.equals("productor", ignoreCase = true)
    val ownProducts = state.products.count { it.producerId == (user?.id ?: firebaseUser?.uid) }
    val attended = state.requests.count { it.producerId == (user?.id ?: firebaseUser?.uid) && it.status != RequestStatus.PENDING }

    Scaffold(
        topBar = { SmallTopBarM3("Perfil") }
    ) { padding ->
        val currentUser = user
        if (firebaseUser == null && currentUser == null) {
            EmptyState(
                title = "Sesión no disponible",
                message = "Vuelve a iniciar sesión para consultar tu perfil",
                icon = Icons.Default.PersonOff,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        // Condición de guardia: Si el rol está vacío (""), nulo o cargando, mostrar únicamente un CircularProgressIndicator centrado
        if (role.isBlank() || isLoading) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val name = currentUser?.name ?: firestoreName.ifBlank { firebaseUser?.email?.substringBefore("@") ?: "Usuario" }
        val location = currentUser?.location ?: firestoreLocation.ifBlank { "Toluca, Estado de México" }
        val businessName = currentUser?.businessName ?: firestoreBusiness

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            name.take(2).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("profile_initials")
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("profile_name"))
                        Text(
                            if (isProducer) businessName.ifBlank { "Productor local" } else "Comprador",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (isProducer) {
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        ProfileStat(ownProducts.toString(), "Productos publicados")
                        ProfileStat(attended.toString(), "Solicitudes atendidas")
                    }
                }
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Column {
                        ProfileMenuItem(Icons.Default.Edit, "Editar perfil", false, true, "profile_edit") {
                            navController.navigate(Screen.EditProfile.route)
                        }
                        ProfileMenuItem(
                            if (isProducer) Icons.Default.Agriculture else Icons.Default.Store,
                            if (isProducer) "Mi información" else "Información de la cuenta",
                            false,
                            true,
                            "profile_information"
                        ) { navController.navigate(Screen.ProfileInfo.route) }
                        ProfileMenuItem(Icons.Default.Settings, "Configuración", false, true, "profile_settings") {
                            navController.navigate(Screen.Settings.route)
                        }
                        ProfileMenuItem(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", true, false, "profile_logout") {
                            viewModel.logout {
                                try {
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                    authViewModel.signOut()
                                } catch (_: Exception) {}
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
                Text(
                    "Cosecha Directa · v${BuildConfig.VERSION_NAME}",
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EditProfileScreen(navController: NavController, viewModel: AuthViewModel) {
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

    var name by rememberSaveable { mutableStateOf(firebaseUser?.displayName.orEmpty()) }
    var location by rememberSaveable { mutableStateOf("") }
    var businessName by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(firebaseUser?.uid) {
        val uid = firebaseUser?.uid
        if (uid != null) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .get()
                    .await()
                if (doc.exists()) {
                    name = doc.getString("nombre") ?: firebaseUser.displayName.orEmpty()
                    location = doc.getString("ubicacion").orEmpty()
                    businessName = doc.getString("negocio").orEmpty()
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(topBar = { SmallTopBarM3("Editar perfil", navController::popBackStack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (firebaseUser == null) {
                ErrorBanner("No hay una sesión activa")
                return@Scaffold
            }
            M3Field("Nombre", name, { name = it }, testTag = "edit_profile_name", enabled = !saving)
            M3Field("Ubicación", location, { location = it }, testTag = "edit_profile_location", enabled = !saving)
            M3Field("Nombre del negocio", businessName, { businessName = it }, testTag = "edit_profile_business", enabled = !saving)
            Text("Correo", style = MaterialTheme.typography.labelLarge)
            Text(firebaseUser.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("edit_profile_email"))
            error?.let { ErrorBanner(it) }
            FilledBtn(
                text = if (saving) "Guardando…" else "Guardar cambios",
                onClick = {
                    error = when {
                        name.isBlank() -> "Ingresa tu nombre"
                        else -> null
                    }
                    if (error == null) {
                        saving = true
                        viewModel.updateFirestoreProfile(name, location, businessName) { success, msg ->
                            saving = false
                            if (success) {
                                navController.popBackStack()
                            } else {
                                error = msg
                            }
                        }
                    }
                },
                enabled = !saving,
                icon = Icons.Default.Save,
                testTag = "edit_profile_save"
            )
        }
    }
}

@Composable
fun ProfileInfoScreen(navController: NavController, state: AppState) {
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    var firestoreRole by remember { mutableStateOf("") }
    var firestoreName by remember { mutableStateOf(firebaseUser?.displayName ?: "Usuario") }
    var firestoreLocation by remember { mutableStateOf("Toluca, Estado de México") }
    var firestoreBusiness by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(firebaseUser?.uid) {
        val uid = firebaseUser?.uid
        if (uid != null) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .get()
                    .await()
                if (doc.exists()) {
                    firestoreRole = doc.getString("rol") ?: ""
                    firestoreName = doc.getString("nombre") ?: firebaseUser.displayName ?: "Usuario"
                    firestoreLocation = doc.getString("ubicacion") ?: "Toluca, Estado de México"
                    firestoreBusiness = doc.getString("negocio").orEmpty()
                }
            } catch (_: Exception) {}
        }
        isLoading = false
    }

    val user = state.currentUser
    val role = if (user != null) {
        if (user.role == UserRole.PRODUCER) "productor" else "comprador"
    } else {
        firestoreRole
    }

    Scaffold(topBar = { SmallTopBarM3("Mi información", navController::popBackStack) }) { padding ->
        if (firebaseUser == null) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ErrorBanner("No hay una sesión activa")
            }
            return@Scaffold
        }

        // Condición de guardia
        if (role.isBlank() || isLoading) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val name = user?.name ?: firestoreName
        val email = user?.email ?: firebaseUser.email.orEmpty()
        val roleStr = if (role.equals("productor", ignoreCase = true)) "Productor" else "Comprador"
        val location = user?.location ?: firestoreLocation
        val businessName = user?.businessName ?: firestoreBusiness

        val ownProducts = state.products.filter { it.producerId == (user?.id ?: firebaseUser.uid) }
        val ownRequests = state.requests.filter {
            if (role.equals("productor", ignoreCase = true)) it.producerId == (user?.id ?: firebaseUser.uid) else it.buyerId == (user?.id ?: firebaseUser.uid)
        }

        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard("Nombre", name)
            InfoCard("Correo", email)
            InfoCard("Rol", roleStr)
            InfoCard("Ubicación", location)
            if (roleStr == "Productor") {
                InfoCard("Negocio", businessName.ifBlank { "Sin nombre comercial" })
                InfoCard("Productos publicados", ownProducts.size.toString())
            }
            InfoCard("Solicitudes", ownRequests.size.toString())
            InfoBanner("Los datos de esta versión se sincronizan con Firebase Firestore.")
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController, state: AppState, viewModel: AppViewModel) {
    val settings = state.settings
    Scaffold(topBar = { SmallTopBarM3("Configuración", navController::popBackStack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Apariencia", style = MaterialTheme.typography.titleMedium)
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> "Usar configuración del sistema"
                    ThemeMode.LIGHT -> "Tema claro"
                    ThemeMode.DARK -> "Tema oscuro"
                }
                Surface(
                    onClick = { viewModel.updateSettings(settings.copy(themeMode = mode)) },
                    modifier = Modifier.fillMaxWidth().testTag("theme_${mode.name.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (settings.themeMode == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = settings.themeMode == mode, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { viewModel.updateSettings(settings.copy(notificationsEnabled = !settings.notificationsEnabled)) }
                    .testTag("notifications_setting"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Notificaciones", style = MaterialTheme.typography.titleMedium)
                    Text("Avisos de solicitudes y cambios de estado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = null
                )
            }
            InfoBanner("La preferencia de notificaciones se guarda localmente. La entrega de notificaciones push requiere configurar un servicio remoto.")
        }
    }
}

@Composable
fun ProducerPublicProfileScreen(navController: NavController, state: AppState, producerId: String) {
    val profile = state.producerProfiles.firstOrNull { it.id == producerId }
        ?: state.users.firstOrNull { it.id == producerId && it.role == UserRole.PRODUCER }?.let {
            ProducerProfile(it.id, it.name, it.location, it.businessName.ifBlank { "Productor local" })
        }
    Scaffold(topBar = { SmallTopBarM3("Perfil del productor", navController::popBackStack) }) { padding ->
        if (profile == null) {
            EmptyState("Productor no encontrado", "Es posible que el perfil ya no esté disponible", Icons.Default.PersonSearch, Modifier.padding(padding))
        } else {
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Text(profile.name.take(2).uppercase(), style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("public_producer_name"))
                            if (profile.verified) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.Verified, "Productor verificado", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(profile.businessName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(profile.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(count: String, label: String) {
    Column {
        Text(count, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun InfoBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(message, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    isDanger: Boolean,
    showDivider: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).testTag(testTag).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(
                    if (isDanger) MaterialTheme.colorScheme.errorContainer else SurfaceContainerHigh,
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Text(label, Modifier.weight(1f), color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (!isDanger) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showDivider) HorizontalDivider(Modifier.padding(start = 72.dp))
    }
}
