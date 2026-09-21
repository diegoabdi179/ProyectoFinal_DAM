package com.example.harvestdistributionapp.data

import java.text.Normalizer
import java.util.Locale

enum class UserRole {
    PRODUCER,
    BUYER
}

enum class AvailabilityStatus {
    AVAILABLE,
    LIMITED,
    OUT
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: UserRole,
    val location: String = "Toluca, Estado de México",
    val businessName: String = ""
)

data class ProducerProfile(
    val id: String,
    val name: String,
    val location: String,
    val businessName: String = "Productor local",
    val verified: Boolean = false
)

data class Product(
    val id: Int,
    val producerId: String,
    val producerName: String,
    val name: String,
    val quantity: Int,
    val unit: String,
    val pricePerUnit: Double,
    val status: AvailabilityStatus,
    val location: String,
    val availableDate: String,
    val imageUri: String,
    val category: String
)

data class PurchaseRequest(
    val id: Int,
    val buyerId: String,
    val buyerName: String,
    val productId: Int,
    val producerId: String,
    val productName: String,
    val quantity: Int,
    val unit: String,
    val requiredDate: String,
    val status: RequestStatus,
    val pricePerUnit: Double,
    val location: String,
    val message: String = "",
    val productImageUri: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val total: Double
        get() = quantity * pricePerUnit
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true
)

data class AppState(
    val users: List<UserAccount> = emptyList(),
    val producerProfiles: List<ProducerProfile> = seedProducerProfiles(),
    val products: List<Product> = seedProducts(),
    val requests: List<PurchaseRequest> = emptyList(),
    val currentUserId: String? = null,
    val settings: AppSettings = AppSettings()
) {
    val currentUser: UserAccount?
        get() = users.firstOrNull { it.id == currentUserId }
}

data class ProductDraft(
    val name: String,
    val quantity: Int,
    val unit: String,
    val pricePerUnit: Double,
    val location: String,
    val availableDate: String,
    val imageUri: String,
    val category: String
)

data class RequestDraft(
    val productId: Int,
    val quantity: Int,
    val requiredDate: String,
    val location: String,
    val message: String
)

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val message: String) : AppResult<Nothing>
}

object InputValidator {
    private val emailRegex = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

    fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    fun validateEmail(email: String): String? = when {
        normalizeEmail(email).isBlank() -> "Ingresa tu correo electrónico"
        !emailRegex.matches(normalizeEmail(email)) -> "Ingresa un correo electrónico válido"
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
        password.none(Char::isLetter) -> "La contraseña debe contener al menos una letra"
        password.none(Char::isDigit) -> "La contraseña debe contener al menos un número"
        else -> null
    }

    fun decrementQuantity(quantity: Int, step: Int = 5, minimum: Int = 1): Int =
        (quantity - step).coerceAtLeast(minimum)

    fun normalizeSearch(value: String): String = Normalizer
        .normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

    fun matchesSearch(product: Product, query: String): Boolean {
        val needle = normalizeSearch(query)
        if (needle.isBlank()) return true
        return listOf(product.name, product.category, product.producerName, product.location)
            .any { normalizeSearch(it).contains(needle) }
    }
}

fun formatMoney(value: Double): String {
    val rendered = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
    return "$$rendered"
}

fun Product.displayPrice(): String = "${formatMoney(pricePerUnit)} / $unit"

fun availabilityForQuantity(quantity: Int): AvailabilityStatus = when {
    quantity <= 0 -> AvailabilityStatus.OUT
    quantity <= 25 -> AvailabilityStatus.LIMITED
    else -> AvailabilityStatus.AVAILABLE
}

fun seedProducerProfiles(): List<ProducerProfile> = listOf(
    ProducerProfile("producer-juan", "Juan García", "Toluca, Estado de México", "Huerto García", true),
    ProducerProfile("producer-maria", "María López", "Metepec, Estado de México", "La Milpa", true),
    ProducerProfile("producer-roberto", "Roberto Ríos", "Calimaya, Estado de México", "Apiario Ríos", true),
    ProducerProfile("producer-ana", "Ana Martínez", "Zinacantepec, Estado de México", "Rancho Martínez", true),
    ProducerProfile("producer-carlos", "Carlos Vega", "Lerma, Estado de México", "Fresas del Valle", false)
)

fun seedProducts(): List<Product> = listOf(
    Product(1, "producer-juan", "Juan García", "Jitomate Saladette", 80, "kg", 18.0, availabilityForQuantity(80), "Toluca", futureIsoDate(1), "https://images.unsplash.com/photo-1546554137-f86b9593a222?w=800&h=600&fit=crop&auto=format", "verduras"),
    Product(2, "producer-maria", "María López", "Lechuga Orejona", 40, "piezas", 12.0, availabilityForQuantity(40), "Metepec", futureIsoDate(1), "https://images.unsplash.com/photo-1622205313162-be1d5712a43f?w=800&h=600&fit=crop&auto=format", "verduras"),
    Product(3, "producer-roberto", "Roberto Ríos", "Miel artesanal", 25, "frascos", 95.0, availabilityForQuantity(25), "Calimaya", futureIsoDate(4), "https://images.unsplash.com/photo-1558642452-9d2a7deb7f62?w=800&h=600&fit=crop&auto=format", "otros"),
    Product(4, "producer-ana", "Ana Martínez", "Aguacate Hass", 60, "kg", 35.0, availabilityForQuantity(60), "Zinacantepec", futureIsoDate(2), "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=800&h=600&fit=crop&auto=format", "frutas"),
    Product(5, "producer-carlos", "Carlos Vega", "Fresa de temporada", 30, "kg", 45.0, availabilityForQuantity(30), "Lerma", futureIsoDate(1), "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=800&h=600&fit=crop&auto=format", "frutas")
)
