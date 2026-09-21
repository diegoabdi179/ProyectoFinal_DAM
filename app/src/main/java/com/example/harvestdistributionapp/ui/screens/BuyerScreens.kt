package com.example.harvestdistributionapp.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

@Composable
fun BuyerBottomBar(navController: NavController, currentRoute: String?) {
    val destinations = listOf(
        Triple(Screen.BuyerHome.route, "Inicio", Icons.Default.Home),
        Triple(Screen.BuyerSearch.route, "Buscar", Icons.Default.Search),
        Triple(Screen.BuyerMyRequests.route, "Solicitudes", Icons.AutoMirrored.Filled.ReceiptLong),
        Triple(Screen.Profile.route, "Perfil", Icons.Default.Person)
    )
    NavigationBar {
        destinations.forEach { (route, label, icon) ->
            NavigationBarItem(
                modifier = Modifier.testTag("buyer_nav_$route"),
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == route,
                onClick = {
                    if (route == Screen.BuyerHome.route) {
                        navController.navigate(route) {
                            popUpTo(Screen.BuyerHome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(route) {
                            popUpTo(Screen.BuyerHome.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun BuyerHomeScreen(navController: NavController, state: AppState) {
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val products = state.products.filter { product ->
        product.status != AvailabilityStatus.OUT && (selectedCategory == null || product.category == selectedCategory)
    }
    val user = state.currentUser
    val categories = listOf("frutas" to "Frutas", "verduras" to "Verduras", "otros" to "Otros")

    Scaffold(
        bottomBar = { BuyerBottomBar(navController, Screen.BuyerHome.route) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        M3Logo()
                        Surface(
                            onClick = { navController.navigate(Screen.Profile.route) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp).testTag("buyer_avatar")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(user?.name?.take(2)?.uppercase() ?: "--", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Buenos días", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${user?.name ?: "Comprador"} 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        onClick = { navController.navigate(Screen.BuyerSearch.route) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("home_search")
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text("Buscar producto, productor o ubicación…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Surface(
                    onClick = { navController.navigate(Screen.BuyerPostNeed.route) },
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().testTag("post_need"),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Publicar una necesidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Busca productores usando una cantidad y fecha reales")
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipM3("Todos", selectedCategory == null, { selectedCategory = null }, testTag = "category_all")
                    categories.forEach { (id, label) ->
                        FilterChipM3(label, selectedCategory == id, { selectedCategory = id }, testTag = "category_$id")
                    }
                }
            }
            item {
                Text("Disponible cerca de ti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (products.isEmpty()) {
                item { EmptyState("Sin resultados", "Prueba otra categoría", Icons.Default.SearchOff) }
            } else {
                items(products, key = Product::id) { product ->
                    BuyerProductCard(product) { navController.navigate(Screen.BuyerProductDetail.createRoute(product.id)) }
                }
            }
        }
    }
}

@Composable
private fun BuyerProductCard(product: Product, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.imageUri,
                contentDescription = "Foto de ${product.name}",
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(product.displayPrice(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("${product.quantity} ${product.unit} disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${product.location} · ${product.producerName}", style = MaterialTheme.typography.bodySmall)
            }
            AvailChip(product.status)
        }
    }
}

@Composable
fun BuyerSearchScreen(navController: NavController, state: AppState) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var localOnly by rememberSaveable { mutableStateOf(false) }
    var maxPrice by rememberSaveable { mutableStateOf<Double?>(null) }
    var availability by rememberSaveable { mutableStateOf<AvailabilityStatus?>(null) }
    val localCity = state.currentUser?.location?.substringBefore(',')?.trim().orEmpty()
    val categories = listOf<String?>(null) + state.products.map(Product::category).distinct().sorted()
    val prices = listOf<Double?>(null, 20.0, 50.0, 100.0)
    val statuses = listOf<AvailabilityStatus?>(null, AvailabilityStatus.AVAILABLE, AvailabilityStatus.LIMITED, AvailabilityStatus.OUT)
    val products = state.products.filter { product ->
        InputValidator.matchesSearch(product, query) &&
            (category == null || product.category == category) &&
            (!localOnly || InputValidator.normalizeSearch(product.location).contains(InputValidator.normalizeSearch(localCity))) &&
            (maxPrice?.let { product.pricePerUnit <= it } ?: true) &&
            (availability == null || product.status == availability)
    }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding().padding(bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Buscar producto…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("buyer_search_input"),
                        trailingIcon = if (query.isNotBlank()) {
                            { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda") } }
                        } else null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipM3(
                        label = category?.replaceFirstChar(Char::uppercase) ?: "Categoría",
                        active = category != null,
                        onClick = { category = categories[(categories.indexOf(category) + 1) % categories.size] },
                        testTag = "filter_category"
                    )
                    FilterChipM3(
                        label = if (localOnly) "Mi municipio" else "Ubicación",
                        active = localOnly,
                        onClick = { localOnly = !localOnly },
                        testTag = "filter_distance"
                    )
                    FilterChipM3(
                        label = maxPrice?.let { "Hasta ${formatMoney(it)}" } ?: "Precio",
                        active = maxPrice != null,
                        onClick = { maxPrice = prices[(prices.indexOf(maxPrice) + 1) % prices.size] },
                        testTag = "filter_price"
                    )
                    FilterChipM3(
                        label = when (availability) {
                            AvailabilityStatus.AVAILABLE -> "Disponible"
                            AvailabilityStatus.LIMITED -> "Limitado"
                            AvailabilityStatus.OUT -> "Agotado"
                            null -> "Disponibilidad"
                        },
                        active = availability != null,
                        onClick = { availability = statuses[(statuses.indexOf(availability) + 1) % statuses.size] },
                        testTag = "filter_availability"
                    )
                }
            }
        },
        bottomBar = { BuyerBottomBar(navController, Screen.BuyerSearch.route) }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("${products.size} PRODUCTOS ENCONTRADOS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (products.isEmpty()) item { EmptyState("Sin coincidencias", "Ajusta la búsqueda o los filtros", Icons.Default.SearchOff) }
            items(products, key = Product::id) { product ->
                BuyerProductCard(product) { navController.navigate(Screen.BuyerProductDetail.createRoute(product.id)) }
            }
        }
    }
}

@Composable
fun BuyerProductDetailScreen(navController: NavController, state: AppState, productId: Int) {
    val product = state.products.firstOrNull { it.id == productId }
    if (product == null) {
        Scaffold(topBar = { SmallTopBarM3("Producto", onBack = navController::popBackStack) }) { padding ->
            EmptyState("Producto no encontrado", "Es posible que haya sido eliminado", modifier = Modifier.padding(padding))
        }
        return
    }
    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                FilledBtn(
                    "Solicitar producto",
                    onClick = { navController.navigate(Screen.BuyerRequest.createRoute(product.id)) },
                    enabled = product.status != AvailabilityStatus.OUT && product.quantity > 0,
                    icon = Icons.Default.ShoppingCart,
                    testTag = "request_product",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Box {
                AsyncImage(product.imageUri, "Foto de ${product.name}", Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Crop)
                IconButton(
                    onClick = navController::popBackStack,
                    modifier = Modifier.statusBarsPadding().padding(16.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar") }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("product_detail_name"))
                        Text(product.displayPrice(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    AvailChip(product.status)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailLine("Disponibles", "${product.quantity} ${product.unit}")
                        DetailLine("Ubicación", product.location)
                        DetailLine("Fecha", product.availableDate)
                    }
                }
                Text("PRODUCTOR", style = MaterialTheme.typography.labelSmall)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Text(product.producerName.take(2).uppercase())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(product.producerName, style = MaterialTheme.typography.titleMedium)
                            Text(product.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextBtn("Ver perfil", { navController.navigate(Screen.ProducerProfile.createRoute(product.producerId)) }, testTag = "view_producer_profile")
                    }
                }
            }
        }
    }
}

@Composable
fun BuyerRequestScreen(navController: NavController, state: AppState, viewModel: AppViewModel, productId: Int) {
    val product = state.products.firstOrNull { it.id == productId }
    if (product == null) {
        Scaffold(topBar = { SmallTopBarM3("Solicitar producto", onBack = navController::popBackStack) }) { padding ->
            EmptyState("Producto no encontrado", "Regresa y selecciona otro producto", modifier = Modifier.padding(padding))
        }
        return
    }
    var quantity by rememberSaveable(product.id) { mutableIntStateOf(minOf(5, product.quantity.coerceAtLeast(1))) }
    var date by rememberSaveable { mutableStateOf(futureIsoDate()) }
    var location by rememberSaveable { mutableStateOf(state.currentUser?.location.orEmpty()) }
    var message by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    Scaffold(topBar = { SmallTopBarM3("Solicitar producto", onBack = navController::popBackStack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            BuyerProductSummary(product)
            Text("Cantidad que necesitas", style = MaterialTheme.typography.titleMedium)
            QuantitySelector(quantity, product.unit, { quantity = it; error = null }, maximum = product.quantity, testTagPrefix = "request_quantity")
            Text("Estimado: ${formatMoney(quantity * product.pricePerUnit)} MXN", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DateField("Fecha requerida", date, { date = it; error = null }, testTag = "request_date")
            M3Field("Ubicación de entrega o recolección", location, { location = it; error = null }, trailingIcon = Icons.Default.LocationOn, testTag = "request_location")
            M3Field(
                "Mensaje para el productor (opcional)",
                message,
                { message = it },
                singleLine = false,
                modifier = Modifier.heightIn(min = 120.dp),
                testTag = "request_message"
            )
            error?.let { ErrorBanner(it) }
            FilledBtn(
                text = if (submitting) "Enviando…" else "Enviar solicitud",
                enabled = !submitting && quantity in 1..product.quantity && location.isNotBlank() && date.isNotBlank(),
                icon = Icons.AutoMirrored.Filled.Send,
                testTag = "request_submit",
                onClick = {
                    submitting = true
                    viewModel.createRequest(RequestDraft(product.id, quantity, date, location, message)) { result ->
                        submitting = false
                        when (result) {
                            is AppResult.Error -> error = result.message
                            is AppResult.Success -> navController.navigate(Screen.BuyerRequestSent.createRoute(result.value.id)) {
                                popUpTo(Screen.BuyerRequest.pattern) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun BuyerRequestSentScreen(navController: NavController, state: AppState, requestId: Int) {
    val request = state.requests.firstOrNull { it.id == requestId }
    if (request == null) {
        Scaffold { padding -> EmptyState("Solicitud no encontrada", "Consulta Mis solicitudes", modifier = Modifier.padding(padding)) }
        return
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("Solicitud enviada", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("request_sent_title"))
        Text("El productor recibió los datos capturados.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(request.productName, style = MaterialTheme.typography.titleMedium)
                DetailLine("Cantidad", "${request.quantity} ${request.unit}")
                DetailLine("Fecha requerida", request.requiredDate)
                DetailLine("Ubicación", request.location)
                DetailLine("Total estimado", "${formatMoney(request.total)} MXN")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estado")
                    ReqChip(request.status)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        FilledBtn(
            "Ver mis solicitudes",
            onClick = {
                navController.navigate(Screen.BuyerMyRequests.route) {
                    popUpTo(Screen.BuyerHome.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            testTag = "view_my_requests"
        )
        Spacer(Modifier.height(12.dp))
        OutlinedBtn(
            "Volver al inicio",
            onClick = {
                navController.navigate(Screen.BuyerHome.route) {
                    popUpTo(Screen.BuyerHome.route) { inclusive = true }
                    launchSingleTop = true
                }
            },
            icon = Icons.Default.Home,
            testTag = "back_to_home"
        )
    }
}

@Composable
fun BuyerMyRequestsScreen(navController: NavController, state: AppState) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Todas", "Pendientes", "Aceptadas", "Rechazadas")
    val ownRequests = state.requests.filter { it.buyerId == state.currentUserId }
    val filtered = ownRequests.filter { request ->
        when (selectedTab) {
            1 -> request.status == RequestStatus.PENDING
            2 -> request.status == RequestStatus.ACCEPTED
            3 -> request.status == RequestStatus.REJECTED
            else -> true
        }
    }
    Scaffold(
        topBar = { SmallTopBarM3("Mis solicitudes") },
        bottomBar = { BuyerBottomBar(navController, Screen.BuyerMyRequests.route) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }
            if (filtered.isEmpty()) {
                EmptyState("Sin solicitudes", "Las solicitudes enviadas aparecerán aquí", Icons.AutoMirrored.Filled.ReceiptLong)
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = PurchaseRequest::id) { request -> BuyerRequestCard(request) }
                }
            }
        }
    }
}

@Composable
private fun BuyerRequestCard(request: PurchaseRequest) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("buyer_request_${request.id}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(request.productImageUri, "Foto de ${request.productName}", Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(request.productName, style = MaterialTheme.typography.titleMedium)
                Text("${request.quantity} ${request.unit} · ${request.requiredDate}")
                Text(formatMoney(request.total), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            ReqChip(request.status)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerPostNeedScreen(navController: NavController, state: AppState) {
    val productOptions = state.products.filter { it.quantity > 0 }.distinctBy { InputValidator.normalizeSearch(it.name) }
    var expanded by remember { mutableStateOf(false) }
    var selectedProductId by rememberSaveable { mutableIntStateOf(-1) }
    val selectedProduct = productOptions.firstOrNull { it.id == selectedProductId }
    var quantity by rememberSaveable { mutableIntStateOf(5) }
    var date by rememberSaveable { mutableStateOf(futureIsoDate()) }
    var location by rememberSaveable { mutableStateOf(state.currentUser?.location.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { SmallTopBarM3("¿Qué necesitas?", onBack = navController::popBackStack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Selecciona un producto y los datos que realmente necesitas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedProduct?.name.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Producto") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("need_product_selector")
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    productOptions.forEach { product ->
                        DropdownMenuItem(
                            text = { Text(product.name) },
                            onClick = { selectedProductId = product.id; quantity = 5.coerceAtMost(product.quantity).coerceAtLeast(1); expanded = false; error = null }
                        )
                    }
                }
            }
            Text("Cantidad que necesitas", style = MaterialTheme.typography.titleMedium)
            QuantitySelector(quantity, selectedProduct?.unit ?: "unidades", { quantity = it; error = null }, testTagPrefix = "need_quantity")
            DateField("Fecha que necesitas", date, { date = it; error = null }, testTag = "need_date")
            M3Field("Ubicación", location, { location = it; error = null }, trailingIcon = Icons.Default.LocationOn, testTag = "need_location")
            error?.let { ErrorBanner(it) }
            FilledBtn(
                "Buscar productores",
                onClick = {
                    val product = selectedProduct
                    error = when {
                        product == null -> "Selecciona un producto"
                        quantity < 1 -> "La cantidad mínima es 1"
                        date.isBlank() -> "Selecciona una fecha"
                        !isIsoDateTodayOrFuture(date) -> "Selecciona una fecha de hoy o posterior"
                        location.isBlank() -> "Ingresa una ubicación"
                        else -> null
                    }
                    if (error == null && product != null) {
                        navController.navigate(Screen.BuyerPostResults.createRoute(product.name, quantity, date, location))
                    }
                },
                icon = Icons.Default.Search,
                testTag = "need_search_submit"
            )
        }
    }
}

@Composable
fun BuyerPostResultsScreen(
    navController: NavController,
    state: AppState,
    productName: String,
    quantity: Int,
    date: String,
    location: String
) {
    val matches = state.products.filter {
        InputValidator.normalizeSearch(it.name) == InputValidator.normalizeSearch(productName) &&
            it.quantity >= quantity && it.status != AvailabilityStatus.OUT
    }
    Scaffold(
        topBar = {
            SmallTopBarM3(
                title = "Productores encontrados",
                onBack = navController::popBackStack
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "$productName · $quantity · $date · $location",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp).testTag("need_results_summary")
                    )
                }
            }
            item { Text("${matches.size} COINCIDENCIAS", style = MaterialTheme.typography.labelMedium) }
            if (matches.isEmpty()) item { EmptyState("Sin coincidencias", "No hay productores con esa cantidad disponible", Icons.Default.SearchOff) }
            items(matches, key = Product::id) { product ->
                Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Text(product.producerName.take(2).uppercase())
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(product.producerName, style = MaterialTheme.typography.titleMedium)
                                Text("${product.quantity} ${product.unit} · ${product.displayPrice()}")
                            }
                            AvailChip(product.status)
                        }
                        FilledBtn(
                            "Solicitar a este productor",
                            onClick = { navController.navigate(Screen.BuyerProductDetail.createRoute(product.id)) },
                            testTag = "need_result_${product.id}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BuyerProductSummary(product: Product) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.imageUri,
                contentDescription = "Foto de ${product.name}",
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(product.displayPrice(), color = MaterialTheme.colorScheme.primary)
                Text(product.producerName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
