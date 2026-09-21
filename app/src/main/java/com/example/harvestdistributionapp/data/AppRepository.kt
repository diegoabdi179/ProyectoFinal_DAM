package com.example.harvestdistributionapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.harvestDataStore by preferencesDataStore(name = "harvest_distribution_state")

interface AppRepository {
    val state: Flow<AppState>

    suspend fun signUp(name: String, email: String, password: String, role: UserRole): AppResult<UserAccount>
    suspend fun login(email: String, password: String): AppResult<UserAccount>
    suspend fun logout()
    suspend fun updateProfile(name: String, location: String, businessName: String): AppResult<UserAccount>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun createProduct(draft: ProductDraft): AppResult<Product>
    suspend fun updateProduct(productId: Int, draft: ProductDraft): AppResult<Product>
    suspend fun createRequest(draft: RequestDraft): AppResult<PurchaseRequest>
    suspend fun updateRequestStatus(requestId: Int, status: RequestStatus): AppResult<PurchaseRequest>
    suspend fun resetForTesting()
}

class PersistentAppRepository(private val context: Context) : AppRepository {
    private val stateKey = stringPreferencesKey("app_state_v2")

    override val state: Flow<AppState> = context.harvestDataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw throwable
        }
        .map { preferences -> AppStateCodec.decode(preferences[stateKey]) }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): AppResult<UserAccount> {
        val cleanName = name.trim()
        if (cleanName.length < 2) return AppResult.Error("Ingresa tu nombre completo")
        InputValidator.validateEmail(email)?.let { return AppResult.Error(it) }
        InputValidator.validatePassword(password)?.let { return AppResult.Error(it) }
        val normalizedEmail = InputValidator.normalizeEmail(email)
        val snapshot = currentState()
        if (snapshot.users.any { it.email == normalizedEmail }) return AppResult.Error("El correo ya está registrado")

        val passwordRecord = PasswordHasher.create(password)
        val user = UserAccount(
            id = normalizedEmail,
            name = cleanName,
            email = normalizedEmail,
            passwordHash = passwordRecord.hash,
            passwordSalt = passwordRecord.salt,
            role = role
        )
        mutate { current ->
            val profiles = if (role == UserRole.PRODUCER) {
                current.producerProfiles.filterNot { it.id == user.id } + ProducerProfile(
                    id = user.id,
                    name = user.name,
                    location = user.location,
                    businessName = user.businessName.ifBlank { "Productor local" },
                    verified = false
                )
            } else current.producerProfiles
            current.copy(users = current.users + user, producerProfiles = profiles, currentUserId = user.id)
        }
        return AppResult.Success(user)
    }

    override suspend fun login(email: String, password: String): AppResult<UserAccount> {
        InputValidator.validateEmail(email)?.let { return AppResult.Error(it) }
        if (password.isBlank()) return AppResult.Error("Ingresa tu contraseña")
        val normalizedEmail = InputValidator.normalizeEmail(email)
        val user = currentState().users.firstOrNull { it.email == normalizedEmail }
            ?: return AppResult.Error("Correo o contraseña incorrectos")
        if (!PasswordHasher.verify(password, user.passwordHash, user.passwordSalt)) {
            return AppResult.Error("Correo o contraseña incorrectos")
        }
        mutate { it.copy(currentUserId = user.id) }
        return AppResult.Success(user)
    }

    override suspend fun logout() {
        mutate { it.copy(currentUserId = null) }
    }

    override suspend fun updateProfile(
        name: String,
        location: String,
        businessName: String
    ): AppResult<UserAccount> {
        val cleanName = name.trim()
        val cleanLocation = location.trim()
        if (cleanName.length < 2) return AppResult.Error("Ingresa un nombre válido")
        if (cleanLocation.length < 2) return AppResult.Error("Ingresa una ubicación válida")
        val snapshot = currentState()
        val currentUser = snapshot.currentUser ?: return AppResult.Error("La sesión expiró")
        val updated = currentUser.copy(name = cleanName, location = cleanLocation, businessName = businessName.trim())
        mutate { state ->
            val profiles = if (updated.role == UserRole.PRODUCER) {
                state.producerProfiles.filterNot { it.id == updated.id } + ProducerProfile(
                    id = updated.id,
                    name = updated.name,
                    location = updated.location,
                    businessName = updated.businessName.ifBlank { "Productor local" },
                    verified = state.producerProfiles.firstOrNull { it.id == updated.id }?.verified ?: false
                )
            } else state.producerProfiles
            state.copy(
                users = state.users.map { if (it.id == updated.id) updated else it },
                producerProfiles = profiles,
                products = state.products.map {
                    if (it.producerId == updated.id) it.copy(producerName = updated.name, location = updated.location.substringBefore(',')) else it
                }
            )
        }
        return AppResult.Success(updated)
    }

    override suspend fun updateSettings(settings: AppSettings) {
        mutate { it.copy(settings = settings) }
    }

    override suspend fun createProduct(draft: ProductDraft): AppResult<Product> {
        validateProductDraft(draft, allowZero = false)?.let { return AppResult.Error(it) }
        val snapshot = currentState()
        val producer = snapshot.currentUser?.takeIf { it.role == UserRole.PRODUCER }
            ?: return AppResult.Error("Inicia sesión como productor")
        val product = Product(
            id = (snapshot.products.maxOfOrNull(Product::id) ?: 0) + 1,
            producerId = producer.id,
            producerName = producer.name,
            name = draft.name.trim(),
            quantity = draft.quantity,
            unit = draft.unit.trim(),
            pricePerUnit = draft.pricePerUnit,
            status = availabilityForQuantity(draft.quantity),
            location = draft.location.trim(),
            availableDate = draft.availableDate,
            imageUri = draft.imageUri,
            category = draft.category.trim().lowercase()
        )
        mutate { it.copy(products = it.products + product) }
        return AppResult.Success(product)
    }

    override suspend fun updateProduct(productId: Int, draft: ProductDraft): AppResult<Product> {
        validateProductDraft(draft, allowZero = true)?.let { return AppResult.Error(it) }
        val snapshot = currentState()
        val producer = snapshot.currentUser?.takeIf { it.role == UserRole.PRODUCER }
            ?: return AppResult.Error("Inicia sesión como productor")
        val existing = snapshot.products.firstOrNull { it.id == productId && it.producerId == producer.id }
            ?: return AppResult.Error("No se encontró el producto")
        val updated = existing.copy(
            name = draft.name.trim(),
            quantity = draft.quantity,
            unit = draft.unit.trim(),
            pricePerUnit = draft.pricePerUnit,
            status = availabilityForQuantity(draft.quantity),
            location = draft.location.trim(),
            availableDate = draft.availableDate,
            imageUri = draft.imageUri,
            category = draft.category.trim().lowercase()
        )
        mutate { state -> state.copy(products = state.products.map { if (it.id == productId) updated else it }) }
        return AppResult.Success(updated)
    }

    override suspend fun createRequest(draft: RequestDraft): AppResult<PurchaseRequest> {
        if (draft.quantity < 1) return AppResult.Error("La cantidad mínima es 1")
        if (draft.requiredDate.isBlank()) return AppResult.Error("Selecciona una fecha")
        if (!isIsoDateTodayOrFuture(draft.requiredDate)) return AppResult.Error("Selecciona una fecha de hoy o posterior")
        if (draft.location.isBlank()) return AppResult.Error("Ingresa la ubicación")
        val snapshot = currentState()
        val buyer = snapshot.currentUser?.takeIf { it.role == UserRole.BUYER }
            ?: return AppResult.Error("Inicia sesión como comprador")
        val product = snapshot.products.firstOrNull { it.id == draft.productId }
            ?: return AppResult.Error("No se encontró el producto")
        if (product.status == AvailabilityStatus.OUT || product.quantity < 1) return AppResult.Error("El producto está agotado")
        if (draft.quantity > product.quantity) return AppResult.Error("Solo hay ${product.quantity} ${product.unit} disponibles")
        val request = PurchaseRequest(
            id = (snapshot.requests.maxOfOrNull(PurchaseRequest::id) ?: 0) + 1,
            buyerId = buyer.id,
            buyerName = buyer.name,
            productId = product.id,
            producerId = product.producerId,
            productName = product.name,
            quantity = draft.quantity,
            unit = product.unit,
            requiredDate = draft.requiredDate,
            status = RequestStatus.PENDING,
            pricePerUnit = product.pricePerUnit,
            location = draft.location.trim(),
            message = draft.message.trim(),
            productImageUri = product.imageUri
        )
        mutate { it.copy(requests = it.requests + request) }
        return AppResult.Success(request)
    }

    override suspend fun updateRequestStatus(requestId: Int, status: RequestStatus): AppResult<PurchaseRequest> {
        if (status == RequestStatus.PENDING) return AppResult.Error("Selecciona Aceptar o Rechazar")
        val snapshot = currentState()
        val producer = snapshot.currentUser?.takeIf { it.role == UserRole.PRODUCER }
            ?: return AppResult.Error("Inicia sesión como productor")
        val existing = snapshot.requests.firstOrNull { it.id == requestId && it.producerId == producer.id }
            ?: return AppResult.Error("No se encontró la solicitud")
        if (existing.status != RequestStatus.PENDING) return AppResult.Error("La solicitud ya fue atendida")
        val product = snapshot.products.firstOrNull { it.id == existing.productId }
            ?: return AppResult.Error("No se encontró el producto")
        if (status == RequestStatus.ACCEPTED && product.quantity < existing.quantity) {
            return AppResult.Error("No hay inventario suficiente para aceptar")
        }
        val updatedRequest = existing.copy(status = status)
        val updatedProducts = if (status == RequestStatus.ACCEPTED) {
            snapshot.products.map {
                if (it.id == product.id) {
                    val remaining = it.quantity - existing.quantity
                    it.copy(quantity = remaining, status = availabilityForQuantity(remaining))
                } else it
            }
        } else snapshot.products
        mutate { state ->
            state.copy(
                requests = state.requests.map { if (it.id == requestId) updatedRequest else it },
                products = updatedProducts
            )
        }
        return AppResult.Success(updatedRequest)
    }

    override suspend fun resetForTesting() {
        context.harvestDataStore.edit { it.clear() }
    }

    private suspend fun currentState(): AppState = state.first()

    private suspend fun mutate(transform: (AppState) -> AppState): AppState {
        var updated = AppState()
        context.harvestDataStore.edit { preferences ->
            updated = transform(AppStateCodec.decode(preferences[stateKey]))
            preferences[stateKey] = AppStateCodec.encode(updated)
        }
        return updated
    }

    private fun validateProductDraft(draft: ProductDraft, allowZero: Boolean): String? = when {
        draft.name.trim().length < 2 -> "Selecciona o ingresa un producto"
        draft.quantity < if (allowZero) 0 else 1 -> "Ingresa una cantidad válida"
        draft.unit.isBlank() -> "Selecciona una unidad"
        draft.pricePerUnit <= 0.0 -> "Ingresa un precio mayor que cero"
        draft.location.trim().length < 2 -> "Ingresa una ubicación"
        draft.availableDate.isBlank() -> "Selecciona una fecha"
        !isIsoDateTodayOrFuture(draft.availableDate) -> "Selecciona una fecha de hoy o posterior"
        draft.imageUri.isBlank() -> "Agrega una foto JPG o PNG"
        draft.category.isBlank() -> "Selecciona una categoría"
        else -> null
    }

}
