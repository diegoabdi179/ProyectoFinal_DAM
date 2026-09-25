package com.example.harvestdistributionapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harvestdistributionapp.data.FirebaseRepository
import com.example.harvestdistributionapp.data.Product
import com.example.harvestdistributionapp.data.PurchaseRequest
import com.example.harvestdistributionapp.data.RequestStatus
import com.example.harvestdistributionapp.data.Result
import com.example.harvestdistributionapp.data.availabilityForQuantity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class ProductViewModel(
    private val repository: FirebaseRepository = FirebaseRepository(),
) : ViewModel() {

    // Estado UiState para gestionar la acción de publicar producto (Idle, Loading, Success, Error)
    private val _publishState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val publishState: StateFlow<UiState<String>> = _publishState.asStateFlow()

    // Estado para la acción de enviar solicitud (Idle, Loading, Success, Error)
    private val _requestState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val requestState: StateFlow<UiState<String>> = _requestState.asStateFlow()

    // Flujo en tiempo real de la lista de productos descargados de la colección "productos" en Firestore
    val productosFlow: StateFlow<List<Product>> = repository.obtenerProductosRealtime()
        .map { result ->
            when (result) {
                is Result.Success -> result.data
                is Result.Error -> emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Solicitudes del productor en tiempo real
    val producerRequestsFlow: StateFlow<List<PurchaseRequest>> = repository.obtenerSolicitudesProductorRealtime(
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    ).map { result ->
        when (result) {
            is Result.Success -> result.data
            is Result.Error -> emptyList()
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    // Solicitudes del comprador en tiempo real
    val buyerRequestsFlow: StateFlow<List<PurchaseRequest>> = repository.obtenerSolicitudesCompradorRealtime(
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    ).map { result ->
        when (result) {
            is Result.Success -> result.data
            is Result.Error -> emptyList()
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList())

    val solicitudesFlow: StateFlow<List<PurchaseRequest>> = buyerRequestsFlow

    fun resetPublishState() {
        _publishState.value = UiState.Idle
    }

    fun resetRequestState() {
        _requestState.value = UiState.Idle
    }

    /**
     * Publica y guarda un nuevo producto en la colección "productos" de Firestore subiendo la imagen a Cloudinary si es local.
     */
    fun publicarProducto(
        name: String,
        quantity: Int,
        unit: String,
        pricePerUnit: Double,
        location: String,
        availableDate: String,
        imageUri: String,
        category: String
    ) {
        val producerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (producerId == null) {
            _publishState.value = UiState.Error("No hay un productor autenticado")
            return
        }

        when {
            name.isBlank() -> {
                _publishState.value = UiState.Error("El nombre del producto no puede estar vacío")
                return
            }
            pricePerUnit <= 0.0 -> {
                _publishState.value = UiState.Error("El precio debe ser mayor a 0")
                return
            }
            quantity <= 0 -> {
                _publishState.value = UiState.Error("La cantidad debe ser mayor a 0")
                return
            }
            location.isBlank() -> {
                _publishState.value = UiState.Error("La ubicación no puede estar vacía")
                return
            }
        }

        _publishState.value = UiState.Loading
        viewModelScope.launch {
            val generatedId = System.currentTimeMillis().toInt().absoluteValue
            var finalImageUri = imageUri
            if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
                val uploadedUrl = repository.subirImagenCloudinary(Uri.parse(imageUri))
                if (uploadedUrl.isNotBlank()) {
                    finalImageUri = uploadedUrl
                }
            }

            val product = Product(
                id = generatedId,
                producerId = producerId,
                productorId = producerId,
                name = name.trim(),
                titulo = name.trim(),
                quantity = quantity,
                stock = quantity,
                unit = unit,
                pricePerUnit = pricePerUnit,
                precio = pricePerUnit,
                location = location.trim(),
                availableDate = availableDate,
                imageUri = finalImageUri,
                category = category,
                status = availabilityForQuantity(quantity),
                descripcion = ""
            )

            when (val result = repository.guardarProduct(product)) {
                is Result.Success -> {
                    _publishState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _publishState.value = UiState.Error(result.exception.localizedMessage ?: "Error al publicar producto")
                }
            }
        }
    }

    /**
     * Actualiza un producto existente en la colección "productos" de Firestore subiendo la imagen a Cloudinary si es local.
     */
    fun actualizarProducto(
        productId: Int,
        name: String,
        quantity: Int,
        unit: String,
        pricePerUnit: Double,
        location: String,
        availableDate: String,
        imageUri: String,
        category: String
    ) {
        val producerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (producerId == null) {
            _publishState.value = UiState.Error("No hay un productor autenticado")
            return
        }

        when {
            name.isBlank() -> {
                _publishState.value = UiState.Error("El nombre del producto no puede estar vacío")
                return
            }
            pricePerUnit <= 0.0 -> {
                _publishState.value = UiState.Error("El precio debe ser mayor a 0")
                return
            }
            quantity <= 0 -> {
                _publishState.value = UiState.Error("La cantidad debe ser mayor a 0")
                return
            }
            location.isBlank() -> {
                _publishState.value = UiState.Error("La ubicación no puede estar vacía")
                return
            }
        }

        _publishState.value = UiState.Loading
        viewModelScope.launch {
            var finalImageUri = imageUri
            if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
                val uploadedUrl = repository.subirImagenCloudinary(Uri.parse(imageUri))
                if (uploadedUrl.isNotBlank()) {
                    finalImageUri = uploadedUrl
                }
            }

            val product = Product(
                id = productId,
                producerId = producerId,
                productorId = producerId,
                name = name.trim(),
                titulo = name.trim(),
                quantity = quantity,
                stock = quantity,
                unit = unit,
                pricePerUnit = pricePerUnit,
                precio = pricePerUnit,
                location = location.trim(),
                availableDate = availableDate,
                imageUri = finalImageUri,
                category = category,
                status = availabilityForQuantity(quantity),
                descripcion = ""
            )

            when (val result = repository.actualizarProduct(productId, product)) {
                is Result.Success -> {
                    _publishState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _publishState.value = UiState.Error(result.exception.localizedMessage ?: "Error al actualizar producto")
                }
            }
        }
    }

    /**
     * Crea y guarda una nueva solicitud de compra en Firestore.
     */
    fun crearSolicitud(
        productId: Int,
        productName: String,
        producerId: String,
        quantity: Int,
        pricePerUnit: Double,
        requiredDate: String,
        location: String,
        message: String,
        productImageUri: String
    ) {
        val buyerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val buyerName = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName ?: "Comprador"
        if (buyerId == null) {
            _requestState.value = UiState.Error("No hay un comprador autenticado")
            return
        }

        when {
            quantity <= 0 -> {
                _requestState.value = UiState.Error("La cantidad debe ser mayor a 0")
                return
            }
            requiredDate.isBlank() -> {
                _requestState.value = UiState.Error("Selecciona una fecha requerida")
                return
            }
            location.isBlank() -> {
                _requestState.value = UiState.Error("Ingresa una ubicación de entrega")
                return
            }
        }

        _requestState.value = UiState.Loading
        viewModelScope.launch {
            val generatedId = System.currentTimeMillis().toInt().absoluteValue
            val request = PurchaseRequest(
                id = generatedId,
                buyerId = buyerId,
                buyerName = buyerName,
                productId = productId,
                producerId = producerId,
                productName = productName,
                quantity = quantity,
                unit = "kg",
                requiredDate = requiredDate,
                status = RequestStatus.PENDING,
                pricePerUnit = pricePerUnit,
                location = location,
                message = message,
                productImageUri = productImageUri
            )

            when (val result = repository.guardarSolicitudFirestore(request)) {
                is Result.Success -> {
                    _requestState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _requestState.value = UiState.Error(result.exception.localizedMessage ?: "Error al enviar la solicitud")
                }
            }
        }
    }

    fun actualizarEstadoSolicitud(requestId: Int, status: RequestStatus, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (repository.actualizarEstadoSolicitud(requestId, status)) {
                is Result.Success -> onComplete(true)
                is Result.Error -> onComplete(false)
            }
        }
    }
}
