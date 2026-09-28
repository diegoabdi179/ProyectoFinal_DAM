package com.example.harvestdistributionapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import com.example.harvestdistributionapp.viewmodel.AuthViewModel
import com.example.harvestdistributionapp.viewmodel.ProductViewModel
import com.example.harvestdistributionapp.viewmodel.UiState
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.harvestdistributionapp.AppViewModel
import com.example.harvestdistributionapp.data.*
import com.example.harvestdistributionapp.ui.components.*
import com.example.harvestdistributionapp.ui.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun ProducerBottomBar(navController: NavController, currentRoute: String?) {
    val destinations = listOf(
        Triple(Screen.ProducerHome.route, "Inicio", Icons.Default.Home),
        Triple(Screen.ProducerProducts.route, "Productos", Icons.Default.Inventory2),
        Triple(Screen.ProducerRequests.route, "Solicitudes", Icons.Default.Notifications),
        Triple(Screen.Profile.route, "Perfil", Icons.Default.Person)
    )
    NavigationBar {
        destinations.forEach { (route, label, icon) ->
            NavigationBarItem(
                modifier = Modifier.testTag("producer_nav_$route"),
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(Screen.ProducerHome.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun ProducerHomeScreen(
    navController: NavController,
    state: AppState,
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val nombreUsuario by authViewModel.nombreUsuario.collectAsState()
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    var firestoreName by remember { mutableStateOf(firebaseUser?.displayName ?: "") }

    LaunchedEffect(firebaseUser?.uid) {
        val uid = firebaseUser?.uid
        if (uid != null && nombreUsuario.isBlank() && firestoreName.isBlank()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .get()
                    .await()
                if (doc.exists()) {
                    firestoreName = doc.getString("nombre") ?: ""
                }
            } catch (_: Exception) {}
        }
    }

    val displayName = nombreUsuario.ifBlank { firestoreName.ifBlank { state.currentUser?.name ?: firebaseUser?.email?.substringBefore("@") ?: "Productor" } }
    val firstName = displayName.trim().split(Regex("\\s+")).firstOrNull() ?: "Productor"

    val initials = run {
        val parts = displayName.trim().split(Regex("\\s+"))
        val first = parts.getOrNull(0)?.take(1)?.uppercase() ?: ""
        val second = parts.getOrNull(1)?.take(1)?.uppercase() ?: ""
        val res = "$first$second"
        if (res.isBlank()) "--" else res
    }

    val allProducts by productViewModel.productosFlow.collectAsState(initial = emptyList())
    val currentProducerId = firebaseUser?.uid ?: state.currentUserId.orEmpty()
    val products = allProducts.filter { it.productorId == currentProducerId || it.producerId == currentProducerId }
    val requests = state.requests.filter { it.producerId == currentProducerId }
    val lowStock = products.count { it.status != AvailabilityStatus.AVAILABLE || (if (it.stock > 0) it.stock <= 25 else it.quantity <= 25) }
    Scaffold(
        bottomBar = { ProducerBottomBar(navController, Screen.ProducerHome.route) },
        floatingActionButton = {
            ExtFAB("Publicar producto", Icons.Default.Add, { navController.navigate(Screen.ProducerPublish.route) }, "publish_fab")
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        M3Logo()
                        Surface(
                            onClick = { navController.navigate(Screen.Profile.route) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp).testTag("producer_avatar")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(initials, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Hola", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$firstName 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(products.size.toString(), "Productos", Modifier.weight(1f), { navController.navigate(Screen.ProducerProducts.route) }, "stat_products")
                    StatCard(requests.count { it.status == RequestStatus.PENDING }.toString(), "Solicitudes", Modifier.weight(1f), { navController.navigate(Screen.ProducerRequests.route) }, "stat_requests")
                    StatCard(lowStock.toString(), "Bajo stock", Modifier.weight(1f), { navController.navigate(Screen.ProducerLowStock.route) }, "stat_low_stock")
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tu disponibilidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextBtn("Ver todo", { navController.navigate(Screen.ProducerProducts.route) })
                }
            }
            if (products.isEmpty()) {
                item { EmptyState("Aún no tienes productos", "Usa Publicar producto para crear tu primera disponibilidad", Icons.Default.AddBusiness) }
            } else {
                items(products.take(3), key = Product::id) { product ->
                    ProducerProductCard(
                        product = product,
                        onClick = { navController.navigate(Screen.ProducerProductDetail.createRoute(product.id)) },
                        onEdit = { navController.navigate(Screen.ProducerEditProduct.createRoute(product.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    count: String,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProducerProductCard(product: Product, onClick: () -> Unit, onEdit: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().testTag("producer_product_${product.id}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(product.imageUri, "Foto de ${product.name}", Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${product.quantity} ${product.unit} · ${product.displayPrice()}")
                Text(product.location, style = MaterialTheme.typography.bodySmall)
                AvailChip(product.status)
            }
            IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_product_${product.id}")) {
                Icon(Icons.Default.Edit, contentDescription = "Editar ${product.name}")
            }
        }
    }
}

@Composable
fun ProducerProductsScreen(
    navController: NavController,
    state: AppState,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    lowStockOnly: Boolean = false
) {
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf<AvailabilityStatus?>(null) }
    
    val allProducts by productViewModel.productosFlow.collectAsState(initial = emptyList())
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val currentProducerId = firebaseUser?.uid ?: state.currentUserId.orEmpty()
    
    val ownProducts = allProducts.filter { it.productorId == currentProducerId || it.producerId == currentProducerId }
    val products = ownProducts.filter { product ->
        val nameToCheck = product.titulo.ifBlank { product.name }
        val categoryToCheck = product.category
        val producerNameToCheck = product.producerName
        val locationToCheck = product.location
        val matchesSearch = query.isBlank() || listOf(nameToCheck, categoryToCheck, producerNameToCheck, locationToCheck)
            .any { InputValidator.normalizeSearch(it).contains(InputValidator.normalizeSearch(query)) }
            
        val isLowStock = product.status != AvailabilityStatus.AVAILABLE || (if (product.stock > 0) product.stock <= 25 else product.quantity <= 25)
        
        matchesSearch &&
            (statusFilter == null || product.status == statusFilter) &&
            (!lowStockOnly || isLowStock)
    }
    Scaffold(
        topBar = {
            SmallTopBarM3(
                if (lowStockOnly) "Bajo stock" else "Mis productos",
                onBack = navController::popBackStack,
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.ProducerPublish.route) }) {
                        Icon(Icons.Default.Add, contentDescription = "Publicar producto")
                    }
                }
            )
        },
        bottomBar = { if (!lowStockOnly) ProducerBottomBar(navController, Screen.ProducerProducts.route) },
        floatingActionButton = {
            ExtFAB("Publicar producto", Icons.Default.Add, { navController.navigate(Screen.ProducerPublish.route) }, "products_publish_fab")
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            M3Field(
                "Buscar producto",
                query,
                { query = it },
                modifier = Modifier.padding(horizontal = 16.dp),
                trailingIcon = Icons.Default.Search,
                testTag = "producer_search_input"
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipM3("Todos", statusFilter == null, { statusFilter = null })
                AvailabilityStatus.entries.forEach { status ->
                    FilterChipM3(
                        label = when (status) {
                            AvailabilityStatus.AVAILABLE -> "Disponibles"
                            AvailabilityStatus.LIMITED -> "Limitados"
                            AvailabilityStatus.OUT -> "Agotados"
                        },
                        active = statusFilter == status,
                        onClick = { statusFilter = status }
                    )
                }
            }
            if (products.isEmpty()) {
                EmptyState("Sin productos", if (query.isBlank()) "Publica una disponibilidad" else "No hay coincidencias", Icons.Default.Inventory2)
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products, key = Product::id) { product ->
                        ProducerProductCard(
                            product,
                            { navController.navigate(Screen.ProducerProductDetail.createRoute(product.id)) },
                            { navController.navigate(Screen.ProducerEditProduct.createRoute(product.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProducerProductDetailScreen(
    navController: NavController,
    state: AppState,
    productId: Int,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val allProducts by productViewModel.productosFlow.collectAsState(initial = emptyList())
    val product = allProducts.firstOrNull { it.id == productId }
    Scaffold(
        topBar = {
            SmallTopBarM3(
                "Detalle del producto",
                onBack = navController::popBackStack,
                actions = {
                    if (product != null) {
                        IconButton(onClick = { navController.navigate(Screen.ProducerEditProduct.createRoute(product.id)) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar producto")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (product == null) {
            EmptyState("Producto no encontrado", "Regresa a Mis productos", modifier = Modifier.padding(padding))
        } else {
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AsyncImage(product.imageUri, "Foto de ${product.name}", Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                Text(product.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("producer_detail_name"))
                AvailChip(product.status)
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProducerDetailLine("Cantidad", "${product.quantity} ${product.unit}")
                        ProducerDetailLine("Precio", product.displayPrice())
                        ProducerDetailLine("Ubicación", product.location)
                        ProducerDetailLine("Fecha", product.availableDate)
                        ProducerDetailLine("Categoría", product.category)
                    }
                }
                FilledBtn("Editar producto", { navController.navigate(Screen.ProducerEditProduct.createRoute(product.id)) }, icon = Icons.Default.Edit, testTag = "detail_edit_product")
            }
        }
    }
}

@Composable
fun ProducerPublishScreen(navController: NavController, state: AppState, viewModel: ProductViewModel) {
    ProductEditorScreen(navController, state, viewModel, existing = null)
}

@Composable
fun ProducerEditProductScreen(navController: NavController, state: AppState, viewModel: ProductViewModel, productId: Int) {
    val allProducts by viewModel.productosFlow.collectAsState(initial = emptyList())
    val product = allProducts.firstOrNull { it.id == productId }
    if (product == null) {
        Scaffold(topBar = { SmallTopBarM3("Editar producto", onBack = navController::popBackStack) }) { padding ->
            EmptyState("Producto no encontrado", "No puedes editar este producto", modifier = Modifier.padding(padding))
        }
    } else ProductEditorScreen(navController, state, viewModel, product)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorScreen(navController: NavController, state: AppState, viewModel: ProductViewModel, existing: Product?) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val productOptions = listOf("Jitomate Saladette", "Lechuga Orejona", "Miel artesanal", "Aguacate Hass", "Fresa de temporada", "Otro")
    val units = listOf("kg", "piezas", "frascos")
    val categories = listOf("frutas", "verduras", "granos", "lácteos", "otros")
    var productMenu by remember { mutableStateOf(false) }
    var unitMenu by remember { mutableStateOf(false) }
    var productSelection by rememberSaveable { mutableStateOf(existing?.name?.takeIf { it in productOptions } ?: if (existing == null) "" else "Otro") }
    var customName by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var quantity by rememberSaveable { mutableStateOf(existing?.quantity?.toString().orEmpty()) }
    var unit by rememberSaveable { mutableStateOf(existing?.unit ?: "kg") }
    var price by rememberSaveable { mutableStateOf(existing?.pricePerUnit?.toString().orEmpty()) }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: state.currentUser?.location?.substringBefore(',').orEmpty()) }
    var availableDate by rememberSaveable { mutableStateOf(existing?.availableDate ?: futureIsoDate()) }
    var imageUri by rememberSaveable { mutableStateOf(existing?.imageUri.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: "verduras") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var photoSaving by remember { mutableStateOf(false) }

    val publishState by viewModel.publishState.collectAsState()

    LaunchedEffect(publishState) {
        when (val st = publishState) {
            is UiState.Error -> {
                error = st.message
                viewModel.resetPublishState()
            }
            is UiState.Success -> {
                viewModel.resetPublishState()
                navController.popBackStack()
            }
            else -> {}
        }
    }

    val submitting = publishState is UiState.Loading

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val size = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L }.getOrDefault(0L)
            when {
                mime !in setOf("image/jpeg", "image/png") -> error = "Selecciona una imagen JPG o PNG"
                size > 10L * 1024 * 1024 -> error = "La imagen supera el límite de 10 MB"
                else -> {
                    photoSaving = true
                    error = null
                    coroutineScope.launch {
                        when (val stored = ProductImageStore.copyToPrivateStorage(context, uri, mime)) {
                            is AppResult.Success -> imageUri = stored.value
                            is AppResult.Error -> error = stored.message
                        }
                        photoSaving = false
                    }
                }
            }
        }
    }

    Scaffold(topBar = { SmallTopBarM3(if (existing == null) "Publicar producto" else "Editar producto", onBack = navController::popBackStack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth().height(180.dp).testTag("product_photo_picker"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ) {
                if (photoSaving) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Guardando foto…")
                    }
                } else if (imageUri.isBlank()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(40.dp))
                        Text("Agregar foto del producto", fontWeight = FontWeight.Bold)
                        Text("JPG o PNG, máximo 10 MB")
                    }
                } else {
                    AsyncImage(imageUri, "Vista previa del producto", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            ExposedDropdownMenuBox(productMenu, { productMenu = !productMenu }) {
                OutlinedTextField(
                    value = productSelection,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Producto") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(productMenu) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("product_name_selector")
                )
                ExposedDropdownMenu(productMenu, { productMenu = false }) {
                    productOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { productSelection = option; if (option != "Otro") customName = option; productMenu = false; error = null }
                        )
                    }
                }
            }
            if (productSelection == "Otro") {
                M3Field("Nombre del producto", customName, { customName = it; error = null }, testTag = "custom_product_name")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                M3Field("Cantidad", quantity, { quantity = it.filter(Char::isDigit); error = null }, modifier = Modifier.weight(1f), type = "number", testTag = "product_quantity")
                M3Field("Precio", price, { value -> price = value.filter { it.isDigit() || it == '.' }; error = null }, modifier = Modifier.weight(1f), type = "decimal", testTag = "product_price")
            }
            ExposedDropdownMenuBox(unitMenu, { unitMenu = !unitMenu }) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unidad") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitMenu) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("product_unit_selector")
                )
                ExposedDropdownMenu(unitMenu, { unitMenu = false }) {
                    units.forEach { option -> DropdownMenuItem({ Text(option) }, { unit = option; unitMenu = false }) }
                }
            }
            Text("Categoría", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { option -> FilterChipM3(option.replaceFirstChar(Char::uppercase), category == option, { category = option; error = null }) }
            }
            M3Field("Ubicación", location, { location = it; error = null }, trailingIcon = Icons.Default.LocationOn, testTag = "product_location")
            DateField("Disponible a partir de", availableDate, { availableDate = it; error = null }, testTag = "product_date")
            error?.let { ErrorBanner(it) }
            val parsedQuantity = quantity.toIntOrNull()
            val parsedPrice = price.toDoubleOrNull()
            val name = if (productSelection == "Otro") customName else productSelection
            val valid = name.isNotBlank() && parsedQuantity != null && parsedQuantity >= (if (existing == null) 1 else 0) &&
                parsedPrice != null && parsedPrice > 0 && location.isNotBlank() && imageUri.isNotBlank() && category.isNotBlank()
            FilledBtn(
                text = when {
                    submitting -> "Guardando…"
                    existing == null -> "Publicar disponibilidad"
                    else -> "Guardar cambios"
                },
                enabled = valid && !submitting && !photoSaving,
                icon = if (existing == null) Icons.Default.Upload else Icons.Default.Save,
                testTag = "product_submit",
                onClick = {
                    if (!valid) { error = "Completa todos los campos y agrega una foto válida"; return@FilledBtn }
                    if (existing == null) {
                        viewModel.publicarProducto(
                            name = name,
                            quantity = parsedQuantity!!,
                            unit = unit,
                            pricePerUnit = parsedPrice!!,
                            location = location,
                            availableDate = availableDate,
                            imageUri = imageUri,
                            category = category
                        )
                    } else {
                        viewModel.actualizarProducto(
                            productId = existing.id,
                            name = name,
                            quantity = parsedQuantity!!,
                            unit = unit,
                            pricePerUnit = parsedPrice!!,
                            location = location,
                            availableDate = availableDate,
                            imageUri = imageUri,
                            category = category
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun ProducerSuccessScreen(
    navController: NavController,
    state: AppState,
    productId: Int,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val allProducts by productViewModel.productosFlow.collectAsState(initial = emptyList())
    val product = allProducts.firstOrNull { it.id == productId }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Text("¡Producto publicado!", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("product_success_title"))
        Spacer(Modifier.height(20.dp))
        if (product != null) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Column(Modifier.padding(16.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleLarge)
                    Text("${product.quantity} ${product.unit} · ${product.displayPrice()}")
                    Text(product.location)
                }
            }
            Spacer(Modifier.height(20.dp))
            TonalBtn("Ver producto", { navController.navigate(Screen.ProducerProductDetail.createRoute(product.id)) { popUpTo(Screen.ProducerSuccess.pattern) { inclusive = true } } }, icon = Icons.Default.Visibility, testTag = "success_view_product")
        } else ErrorBanner("No se encontró el producto publicado")
        Spacer(Modifier.height(12.dp))
        FilledBtn(
            "Volver al inicio",
            onClick = { navController.navigate(Screen.ProducerHome.route) { popUpTo(Screen.ProducerHome.route) { inclusive = true }; launchSingleTop = true } },
            testTag = "success_home"
        )
    }
}

@Composable
fun ProducerRequestsScreen(
    navController: NavController,
    state: AppState,
    viewModel: AppViewModel,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val tabs = listOf("Todas", "Pendientes", "Aceptadas", "Rechazadas")

    val allRequests by productViewModel.producerRequestsFlow.collectAsState(initial = emptyList())
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val currentProducerId = firebaseUser?.uid ?: state.currentUserId.orEmpty()

    val own = allRequests.filter { it.producerId == currentProducerId }
    val filtered = own.filter {
        when (selectedTab) {
            1 -> it.status == RequestStatus.PENDING
            2 -> it.status == RequestStatus.ACCEPTED
            3 -> it.status == RequestStatus.REJECTED
            else -> true
        }
    }
    Scaffold(
        topBar = { SmallTopBarM3("Solicitudes recibidas") },
        bottomBar = { ProducerBottomBar(navController, Screen.ProducerRequests.route) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            message?.let { ErrorBanner(it, Modifier.padding(16.dp)) }
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title -> Tab(selectedTab == index, { selectedTab = index }, text = { Text(title) }) }
            }
            if (filtered.isEmpty()) {
                EmptyState("Sin solicitudes", "Las solicitudes de compradores aparecerán aquí", Icons.Default.NotificationsNone)
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered, key = PurchaseRequest::id) { request ->
                        ProducerRequestCard(
                            request,
                            onOpen = { navController.navigate(Screen.ProducerRequestDetail.createRoute(request.id)) },
                            onStatus = { status ->
                                productViewModel.actualizarEstadoSolicitud(request.id, status) { success ->
                                    if (!success) message = "Error al actualizar estado"
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProducerRequestCard(
    request: PurchaseRequest,
    onOpen: () -> Unit,
    onStatus: (RequestStatus) -> Unit
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("producer_request_${request.id}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Text(request.buyerName.take(2).uppercase())
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(request.buyerName, style = MaterialTheme.typography.titleMedium)
                    Text(request.productName)
                    Text("${request.quantity} ${request.unit} · ${formatMoney(request.total)}")
                }
                ReqChip(request.status)
            }
            if (request.status == RequestStatus.PENDING) {
                RequestActionButtons(request.id, onStatus)
            }
        }
    }
}

@Composable
fun ProducerRequestDetailScreen(navController: NavController, state: AppState, viewModel: AppViewModel, requestId: Int) {
    val request = state.requests.firstOrNull { it.id == requestId && it.producerId == state.currentUserId }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { SmallTopBarM3("Solicitud de compra", onBack = navController::popBackStack) }) { padding ->
        if (request == null) {
            EmptyState("Solicitud no encontrada", "Es posible que ya no exista", modifier = Modifier.padding(padding))
        } else {
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(request.productName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("request_detail_product"))
                ReqChip(request.status)
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProducerDetailLine("Comprador", request.buyerName)
                        ProducerDetailLine("Cantidad", "${request.quantity} ${request.unit}")
                        ProducerDetailLine("Precio", "${formatMoney(request.pricePerUnit)} / ${request.unit}")
                        ProducerDetailLine("Total", formatMoney(request.total))
                        ProducerDetailLine("Fecha", request.requiredDate)
                        ProducerDetailLine("Ubicación", request.location)
                    }
                }
                if (request.message.isNotBlank()) InfoBanner(request.message)
                error?.let { ErrorBanner(it) }
                if (request.status == RequestStatus.PENDING) {
                    RequestActionButtons(request.id) { status ->
                        viewModel.updateRequestStatus(request.id, status) { result -> if (result is AppResult.Error) error = result.message }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestActionButtons(requestId: Int, onStatus: (RequestStatus) -> Unit) {
    var showRejectConfirmation by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledBtn(
            "Aceptar",
            { onStatus(RequestStatus.ACCEPTED) },
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Check,
            testTag = "accept_request_$requestId"
        )
        OutlinedBtn(
            "Rechazar",
            { showRejectConfirmation = true },
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Close,
            testTag = "reject_request_$requestId"
        )
    }
    if (showRejectConfirmation) {
        AlertDialog(
            onDismissRequest = { showRejectConfirmation = false },
            title = { Text("Rechazar solicitud") },
            text = { Text("Esta acción cambiará el estado para el comprador.") },
            confirmButton = {
                TextButton(
                    onClick = { showRejectConfirmation = false; onStatus(RequestStatus.REJECTED) },
                    modifier = Modifier.testTag("confirm_reject_$requestId")
                ) { Text("Rechazar") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRejectConfirmation = false },
                    modifier = Modifier.testTag("cancel_reject_$requestId")
                ) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ProducerDetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
