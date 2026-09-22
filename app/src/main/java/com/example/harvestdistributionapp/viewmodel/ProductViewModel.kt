package com.example.harvestdistributionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harvestdistributionapp.data.FirebaseRepository
import com.example.harvestdistributionapp.data.Product
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

    fun resetPublishState() {
        _publishState.value = UiState.Idle
    }

    /**
     * Publica y guarda un nuevo producto en la colección "productos" de Firestore asignando cada campo de forma individual.
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
                imageUri = imageUri,
                category = category,
                status = availabilityForQuantity(quantity),
                descripcion = "" // Sin concatenaciones raras
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
     * Actualiza un producto existente en la colección "productos" de Firestore usando su ID original.
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
            val product = Product(
                id = productId, // ESTRICTO: Usar el ID original, NUNCA generar uno nuevo al editar
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
                imageUri = imageUri,
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
}
