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


import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.harvestdistributionapp.AppViewModel
import com.example.harvestdistributionapp.viewmodel.ProductViewModel
import com.example.harvestdistributionapp.viewmodel.AuthViewModel
import com.example.harvestdistributionapp.viewmodel.UiState
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import com.example.harvestdistributionapp.data.*
import com.example.harvestdistributionapp.ui.components.*
import com.example.harvestdistributionapp.ui.navigation.Screen

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BuyerMainScreen(
    navController: NavController,
    state: AppState,
    productViewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    appViewModel: AppViewModel
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            val destinations = listOf(
                Pair("Inicio", Icons.Default.Home),
                Pair("Buscar", Icons.Default.Search),
                Pair("Solicitudes", Icons.AutoMirrored.Filled.ReceiptLong),
                Pair("Perfil", Icons.Default.Person)
            )
            NavigationBar {
                destinations.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> BuyerHomeScreen(navController, state, productViewModel, authViewModel)
                1 -> BuyerSearchScreen(navController, state)
                2 -> BuyerMyRequestsScreen(navController, state)
                3 -> ProfileScreen(navController, state, appViewModel, authViewModel)
            }
        }
    }
}


@Composable
fun BuyerHomeScreen(
    navController: NavController,
    state: AppState,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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

    val displayName = nombreUsuario.ifBlank { firestoreName.ifBlank { state.currentUser?.name ?: firebaseUser?.email?.substringBefore("@") ?: "Comprador" } }
    val firstName = displayName.trim().split(Regex("\\s+")).firstOrNull() ?: "Comprador"

    val initials = run {
        val parts = displayName.trim().split(Regex("\\s+"))
        val first = parts.getOrNull(0)?.take(1)?.uppercase() ?: ""
        val second = parts.getOrNull(1)?.take(1)?.uppercase() ?: ""
        val res = "$first$second"
        if (res.isBlank()) "--" else res
    }

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val allProducts by productViewModel.productosFlow.collectAsState(initial = emptyList())
    val products = remember(allProducts, selectedCategory) {
        allProducts.filter { product ->
            val cat = product.category.ifBlank { "verduras" }
            product.status != AvailabilityStatus.OUT && (selectedCategory == null || cat == selectedCategory)
        }
    }
    val categories = listOf("frutas" to "Frutas", "verduras" to "Verduras", "otros" to "Otros")

    Scaffold(
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
                                Text(initials, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Buenos días", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$firstName 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                item {
                    EmptyState(
                        title = "Sin resultados",
                        message = "Prueba cambiando de categoría o explorando todo el catálogo",
                        icon = Icons.Default.SearchOff,
                        actionLabel = "Ver todo el catálogo",
                        actionIcon = Icons.Default.FilterListOff,
                        onActionClick = { selectedCategory = null }
                    )
                }
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
fun BuyerSearchScreen(
    navController: NavController,
    state: AppState,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var localOnly by rememberSaveable { mutableStateOf(false) }
    var maxPrice by rememberSaveable { mutableStateOf<Double?>(null) }
    var availability by rememberSaveable { mutableStateOf<AvailabilityStatus?>(null) }
    val localCity = state.currentUser?.location?.substringBefore(',')?.trim().orEmpty()

    val allProducts by productViewModel.productosFlow.collectAsState(initial = emptyList())
    val categories = listOf<String?>(null) + allProducts.map { it.category.ifBlank { "verduras" } }.distinct().sorted()
    val prices = listOf<Double?>(null, 20.0, 50.0, 100.0)
    val statuses = listOf<AvailabilityStatus?>(null, AvailabilityStatus.AVAILABLE, AvailabilityStatus.LIMITED, AvailabilityStatus.OUT)
    val products = remember(allProducts, query, category, localOnly, maxPrice, availability) {
        allProducts.filter { product ->
            val nameToCheck = product.titulo.ifBlank { product.name }
            val categoryToCheck = product.category.ifBlank { "verduras" }
            val producerNameToCheck = product.producerName
            val locationToCheck = product.location
            val priceToCheck = if (product.precio > 0.0) product.precio else product.pricePerUnit

            val matchesSearch = query.isBlank() || listOf(nameToCheck, categoryToCheck, producerNameToCheck, locationToCheck)
                .any { InputValidator.normalizeSearch(it).contains(InputValidator.normalizeSearch(query)) }

            matchesSearch &&
                (category == null || categoryToCheck == category) &&
                (!localOnly || InputValidator.normalizeSearch(locationToCheck).contains(InputValidator.normalizeSearch(localCity))) &&
                (maxPrice?.let { priceToCheck <= it } ?: true) &&
                (availability == null || product.status == availability)
        }
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
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("${products.size} PRODUCTOS ENCONTRADOS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (products.isEmpty()) {
                item {
                    EmptyState(
                        title = "Sin coincidencias",
                        message = "Ajusta la búsqueda o los filtros para encontrar productos",
                        icon = Icons.Default.SearchOff,
                        actionLabel = "Limpiar filtros",
                        actionIcon = Icons.Default.Clear,
                        onActionClick = {
                            query = ""
                            category = null
                            localOnly = false
                            maxPrice = null
                            availability = null
                        }
                    )
                }
            }
            items(products, key = Product::id) { product ->
                BuyerProductCard(product) { navController.navigate(Screen.BuyerProductDetail.createRoute(product.id)) }
            }
        }
    }
}

@Composable
fun BuyerProductDetailScreen(
    navController: NavController,
    state: AppState,
    productId: Int,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val allProducts by productViewModel.productosFlow.collectAsState(initial = emptyList())
    val product = allProducts.firstOrNull { it.id == productId }
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
                    val pProducer = product.producerName.ifBlank { "Productor" }
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Text(getBuyerInitials(pProducer))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pProducer, style = MaterialTheme.typography.titleMedium)
                            Text(product.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuyerRequestScreen(
    navController: NavController,
    state: AppState,
    viewModel: AppViewModel,
    productId: Int,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val allProducts by productViewModel.productosFlow.collectAsState(initial = emptyList())
    val product = allProducts.firstOrNull { it.id == productId }
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

    val requestState by productViewModel.requestState.collectAsState()

    LaunchedEffect(requestState) {
        when (val st = requestState) {
            is UiState.Error -> {
                error = st.message
                productViewModel.resetRequestState()
            }
            is UiState.Success -> {
                productViewModel.resetRequestState()
                navController.navigate(Screen.BuyerHome.route) {
                    popUpTo(Screen.BuyerHome.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            else -> {}
        }
    }

    val submitting = requestState is UiState.Loading

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
                    if (quantity < 1 || location.isBlank() || date.isBlank()) {
                        error = "Completa todos los campos obligatorios"
                        return@FilledBtn
                    }
                    productViewModel.crearSolicitud(
                        productId = product.id,
                        productName = product.titulo.ifBlank { product.name },
                        producerId = product.productorId.ifBlank { product.producerId },
                        quantity = quantity,
                        pricePerUnit = if (product.precio > 0.0) product.precio else product.pricePerUnit,
                        requiredDate = date,
                        location = location,
                        message = message,
                        productImageUri = product.imageUri
                    )
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
fun BuyerMyRequestsScreen(
    navController: NavController,
    state: AppState,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Todas", "Pendientes", "Aceptadas", "Rechazadas")
    val requests by productViewModel.solicitudesFlow.collectAsState(initial = emptyList())
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val currentBuyerId = firebaseUser?.uid ?: state.currentUserId.orEmpty()

    val ownRequests = remember(requests, currentBuyerId) {
        requests.filter { it.buyerId == currentBuyerId }
    }
    val filtered = remember(ownRequests, selectedTab) {
        ownRequests.filter { request ->
            when (selectedTab) {
                1 -> request.status == RequestStatus.PENDING
                2 -> request.status == RequestStatus.ACCEPTED
                3 -> request.status == RequestStatus.REJECTED
                else -> true
            }
        }
    }
    Scaffold(
        topBar = { SmallTopBarM3("Mis solicitudes") }
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
                EmptyState(
                    title = "Sin solicitudes",
                    message = if (selectedTab == 0) "Las solicitudes enviadas aparecerán aquí" else "No tienes solicitudes en este estado",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    actionLabel = if (selectedTab != 0) "Ver todas" else "Explorar productos",
                    actionIcon = if (selectedTab != 0) Icons.Default.FilterListOff else Icons.Default.Search,
                    onActionClick = {
                        if (selectedTab != 0) selectedTab = 0
                        else navController.navigate(Screen.BuyerSearch.route)
                    }
                )
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

@Composable
fun BuyerPostNeedScreen(
    navController: NavController,
    state: AppState
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableIntStateOf(5) }
    var date by rememberSaveable { mutableStateOf(futureIsoDate()) }
    var location by rememberSaveable { mutableStateOf(state.currentUser?.location.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(topBar = { SmallTopBarM3("¿Qué necesitas?", onBack = navController::popBackStack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Escribe el producto que buscas y los datos requeridos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            M3Field(
                label = "Buscar producto...",
                value = searchQuery,
                onValueChange = { searchQuery = it; error = null },
                trailingIcon = Icons.Default.Search,
                testTag = "need_product_search"
            )
            Text("Cantidad que necesitas", style = MaterialTheme.typography.titleMedium)
            QuantitySelector(quantity, "kg", { quantity = it; error = null }, testTagPrefix = "need_quantity")
            DateField("Fecha que necesitas", date, { date = it; error = null }, testTag = "need_date")
            M3Field("Ubicación", location, { location = it; error = null }, trailingIcon = Icons.Default.LocationOn, testTag = "need_location")
            error?.let { ErrorBanner(it) }
            FilledBtn(
                "Buscar productores",
                onClick = {
                    error = when {
                        searchQuery.isBlank() -> "Ingresa el nombre del producto"
                        quantity < 1 -> "La cantidad mínima es 1"
                        date.isBlank() -> "Selecciona una fecha"
                        !isIsoDateTodayOrFuture(date) -> "Selecciona una fecha de hoy o posterior"
                        location.isBlank() -> "Ingresa una ubicación"
                        else -> null
                    }
                    if (error == null) {
                        navController.navigate(Screen.BuyerPostResults.createRoute(searchQuery.trim(), quantity, date, location))
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
    location: String,
    productViewModel: ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val allProducts by productViewModel.productosFlow.collectAsState(initial = emptyList())
    val matches = allProducts.filter { product ->
        val name = product.titulo.ifBlank { product.name }
        val stock = if (product.stock > 0) product.stock else product.quantity
        InputValidator.normalizeSearch(name) == InputValidator.normalizeSearch(productName) &&
            stock >= quantity && product.status != AvailabilityStatus.OUT
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
                val pProducer = product.producerName.ifBlank { "Productor" }
                val pInitials = if (product.producerName.isNotBlank()) product.producerName.take(2).uppercase() else "PR"
                val pStock = if (product.stock > 0) product.stock else product.quantity
                val pPrice = product.displayPrice()
                Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Text(pInitials)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(pProducer, style = MaterialTheme.typography.titleMedium)
                                Text("$pStock ${product.unit} · $pPrice")
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

private fun getBuyerInitials(name: String): String {
    val clean = name.ifBlank { "Productor" }
    if (clean.equals("Productor", ignoreCase = true)) return "PR"
    val parts = clean.trim().split(Regex("\\s+"))
    val first = parts.getOrNull(0)?.take(1)?.uppercase() ?: ""
    val second = parts.getOrNull(1)?.take(1)?.uppercase() ?: ""
    val res = "$first$second"
    return if (res.isBlank()) "PR" else res
}
