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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.harvestdistributionapp.AppViewModel
import com.example.harvestdistributionapp.BuildConfig
import com.example.harvestdistributionapp.data.*
import com.example.harvestdistributionapp.ui.components.*
import com.example.harvestdistributionapp.ui.navigation.Screen
import com.example.harvestdistributionapp.ui.theme.SurfaceContainerHigh

@Composable
fun ProfileScreen(navController: NavController, state: AppState, viewModel: AppViewModel) {
    val user = state.currentUser
    val isProducer = user?.role == UserRole.PRODUCER
    val ownProducts = state.products.count { it.producerId == user?.id }
    val attended = state.requests.count { it.producerId == user?.id && it.status != RequestStatus.PENDING }

    Scaffold(
        topBar = { SmallTopBarM3("Perfil") },
        bottomBar = {
            if (isProducer) ProducerBottomBar(navController, Screen.Profile.route)
            else BuyerBottomBar(navController, Screen.Profile.route)
        }
    ) { padding ->
        if (user == null) {
            EmptyState(
                title = "Sesión no disponible",
                message = "Vuelve a iniciar sesión para consultar tu perfil",
                icon = Icons.Default.PersonOff,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

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
                            user.name.take(2).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("profile_initials")
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("profile_name"))
                        Text(
                            if (isProducer) user.businessName.ifBlank { "Productor local" } else "Comprador",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(user.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(navController.graph.id) { inclusive = true }
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
fun EditProfileScreen(navController: NavController, state: AppState, viewModel: AppViewModel) {
    val user = state.currentUser
    var name by rememberSaveable(user?.id) { mutableStateOf(user?.name.orEmpty()) }
    var location by rememberSaveable(user?.id) { mutableStateOf(user?.location.orEmpty()) }
    var businessName by rememberSaveable(user?.id) { mutableStateOf(user?.businessName.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { SmallTopBarM3("Editar perfil", navController::popBackStack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (user == null) {
                ErrorBanner("No hay una sesión activa")
                return@Column
            }
            M3Field("Nombre", name, { name = it }, testTag = "edit_profile_name")
            M3Field("Ubicación", location, { location = it }, testTag = "edit_profile_location")
            if (user.role == UserRole.PRODUCER) {
                M3Field("Nombre del negocio", businessName, { businessName = it }, testTag = "edit_profile_business")
            }
            Text("Correo", style = MaterialTheme.typography.labelLarge)
            Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("edit_profile_email"))
            error?.let { ErrorBanner(it) }
            FilledBtn(
                text = if (saving) "Guardando…" else "Guardar cambios",
                onClick = {
                    error = when {
                        name.isBlank() -> "Ingresa tu nombre"
                        location.isBlank() -> "Ingresa tu ubicación"
                        else -> null
                    }
                    if (error == null) {
                        saving = true
                        viewModel.updateProfile(name, location, businessName) { result ->
                            saving = false
                            when (result) {
                                is AppResult.Success -> navController.popBackStack()
                                is AppResult.Error -> error = result.message
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
    val user = state.currentUser
    val ownProducts = state.products.filter { it.producerId == user?.id }
    val ownRequests = state.requests.filter {
        if (user?.role == UserRole.PRODUCER) it.producerId == user.id else it.buyerId == user?.id
    }
    Scaffold(topBar = { SmallTopBarM3("Mi información", navController::popBackStack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (user == null) {
                ErrorBanner("No hay una sesión activa")
                return@Column
            }
            InfoCard("Nombre", user.name)
            InfoCard("Correo", user.email)
            InfoCard("Rol", if (user.role == UserRole.PRODUCER) "Productor" else "Comprador")
            InfoCard("Ubicación", user.location)
            if (user.role == UserRole.PRODUCER) {
                InfoCard("Negocio", user.businessName.ifBlank { "Sin nombre comercial" })
                InfoCard("Productos publicados", ownProducts.size.toString())
            }
            InfoCard("Solicitudes", ownRequests.size.toString())
            InfoBanner("Los datos de esta versión se guardan únicamente en este dispositivo.")
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
    val products = state.products.filter { it.producerId == producerId && it.status != AvailabilityStatus.OUT }
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
                Text("Productos disponibles (${products.size})", style = MaterialTheme.typography.titleMedium)
                products.forEach { product ->
                    Surface(
                        onClick = { navController.navigate(Screen.BuyerProductDetail.createRoute(product.id)) },
                        modifier = Modifier.fillMaxWidth().testTag("public_product_${product.id}"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold)
                                Text("${product.quantity} ${product.unit} · ${product.displayPrice()}")
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = "Ver producto")
                        }
                    }
                }
                if (products.isEmpty()) InfoBanner("Este productor no tiene productos disponibles por ahora.")
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
private fun ProfileMenuItem(
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
            if (!isDanger) Icon(Icons.Default.ChevronRight, "Abrir $label")
        }
        if (showDivider) HorizontalDivider(Modifier.padding(start = 72.dp))
    }
}
