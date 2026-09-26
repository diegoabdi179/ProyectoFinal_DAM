package com.example.harvestdistributionapp.data

import org.json.JSONArray
import org.json.JSONObject

object AppStateCodec {
    fun encode(state: AppState): String = JSONObject().apply {
        put("version", 2)
        put("currentUserId", state.currentUserId ?: JSONObject.NULL)
        put("settings", encodeSettings(state.settings))
        put("users", JSONArray().apply { state.users.forEach { put(encodeUser(it)) } })
        put("producerProfiles", JSONArray().apply { state.producerProfiles.forEach { put(encodeProfile(it)) } })
        put("products", JSONArray().apply { state.products.forEach { put(encodeProduct(it)) } })
        put("requests", JSONArray().apply { state.requests.forEach { put(encodeRequest(it)) } })
    }.toString()

    fun decode(value: String?): AppState {
        if (value.isNullOrBlank()) return AppState()
        return runCatching {
            val root = JSONObject(value)
            AppState(
                users = root.optJSONArray("users").mapObjects(::decodeUser),
                producerProfiles = root.optJSONArray("producerProfiles").mapObjects(::decodeProfile)
                    .ifEmpty { seedProducerProfiles() },
                products = root.optJSONArray("products").mapObjects(::decodeProduct)
                    .ifEmpty { seedProducts() }
                    .map { product ->
                        // Parche para corregir la imagen de la miel que se quedó guardada en el dispositivo
                        if (product.id == 3 && product.imageUri.contains("photo-1587049352846-4a222e784d38")) {
                            product.copy(imageUri = "https://images.unsplash.com/photo-1558642452-9d2a7deb7f62?w=800&h=600&fit=crop&auto=format")
                        } else product
                    },
                requests = root.optJSONArray("requests").mapObjects(::decodeRequest),
                currentUserId = root.optNullableString("currentUserId"),
                settings = root.optJSONObject("settings")?.let(::decodeSettings) ?: AppSettings()
            )
        }.getOrElse { AppState() }
    }

    private fun encodeUser(user: UserAccount) = JSONObject().apply {
        put("id", user.id)
        put("name", user.name)
        put("email", user.email)
        put("passwordHash", user.passwordHash)
        put("passwordSalt", user.passwordSalt)
        put("role", user.role.name)
        put("location", user.location)
        put("businessName", user.businessName)
    }

    private fun decodeUser(json: JSONObject) = UserAccount(
        id = json.getString("id"),
        name = json.getString("name"),
        email = json.getString("email"),
        passwordHash = json.getString("passwordHash"),
        passwordSalt = json.getString("passwordSalt"),
        role = json.optEnum("role", UserRole.BUYER),
        location = json.optString("location", "Toluca, Estado de México"),
        businessName = json.optString("businessName")
    )

    private fun encodeProfile(profile: ProducerProfile) = JSONObject().apply {
        put("id", profile.id)
        put("name", profile.name)
        put("location", profile.location)
        put("businessName", profile.businessName)
        put("verified", profile.verified)
    }

    private fun decodeProfile(json: JSONObject) = ProducerProfile(
        id = json.getString("id"),
        name = json.getString("name"),
        location = json.optString("location"),
        businessName = json.optString("businessName", "Productor local"),
        verified = json.optBoolean("verified")
    )

    private fun encodeProduct(product: Product) = JSONObject().apply {
        put("id", product.id)
        put("productorId", product.productorId.ifBlank { product.producerId })
        put("titulo", product.titulo.ifBlank { product.name })
        put("descripcion", product.descripcion)
        put("precio", if (product.precio > 0.0) product.precio else product.pricePerUnit)
        put("stock", if (product.stock > 0) product.stock else product.quantity)
        put("producerId", product.producerId)
        put("producerName", product.producerName)
        put("name", product.name)
        put("quantity", product.quantity)
        put("unit", product.unit)
        put("pricePerUnit", product.pricePerUnit)
        put("status", product.status.name)
        put("location", product.location)
        put("availableDate", product.availableDate)
        put("imageUri", product.imageUri)
        put("category", product.category)
    }

    private fun decodeProduct(json: JSONObject) = Product(
        id = json.optInt("id"),
        productorId = json.optString("productorId", json.optString("producerId")),
        titulo = json.optString("titulo", json.optString("name")),
        descripcion = json.optString("descripcion"),
        precio = json.optDouble("precio", json.optDouble("pricePerUnit")),
        stock = json.optInt("stock", json.optInt("quantity")),
        producerId = json.optString("producerId", json.optString("productorId")),
        producerName = json.optString("producerName"),
        name = json.optString("name", json.optString("titulo")),
        quantity = json.optInt("quantity", json.optInt("stock")),
        unit = json.optString("unit", "kg"),
        pricePerUnit = json.optDouble("pricePerUnit", json.optDouble("precio")),
        status = json.optEnum("status", AvailabilityStatus.AVAILABLE),
        location = json.optString("location"),
        availableDate = json.optString("availableDate"),
        imageUri = json.optString("imageUri"),
        category = json.optString("category", "otros")
    )

    private fun encodeRequest(request: PurchaseRequest) = JSONObject().apply {
        put("id", request.id)
        put("buyerId", request.buyerId)
        put("buyerName", request.buyerName)
        put("productId", request.productId)
        put("producerId", request.producerId)
        put("productName", request.productName)
        put("quantity", request.quantity)
        put("unit", request.unit)
        put("requiredDate", request.requiredDate)
        put("status", request.status.name)
        put("pricePerUnit", request.pricePerUnit)
        put("location", request.location)
        put("message", request.message)
        put("productImageUri", request.productImageUri)
        put("createdAtEpochMillis", request.createdAtEpochMillis)
    }

    private fun decodeRequest(json: JSONObject) = PurchaseRequest(
        id = json.getInt("id"),
        buyerId = json.getString("buyerId"),
        buyerName = json.getString("buyerName"),
        productId = json.getInt("productId"),
        producerId = json.getString("producerId"),
        productName = json.getString("productName"),
        quantity = json.getInt("quantity"),
        unit = json.getString("unit"),
        requiredDate = json.getString("requiredDate"),
        status = json.optEnum("status", RequestStatus.PENDING),
        pricePerUnit = json.getDouble("pricePerUnit"),
        location = json.getString("location"),
        message = json.optString("message"),
        productImageUri = json.optString("productImageUri"),
        createdAtEpochMillis = json.optLong("createdAtEpochMillis", System.currentTimeMillis())
    )

    private fun encodeSettings(settings: AppSettings) = JSONObject().apply {
        put("themeMode", settings.themeMode.name)
        put("notificationsEnabled", settings.notificationsEnabled)
    }

    private fun decodeSettings(json: JSONObject) = AppSettings(
        themeMode = json.optEnum("themeMode", ThemeMode.SYSTEM),
        notificationsEnabled = json.optBoolean("notificationsEnabled", true)
    )

    private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T =
        runCatching { enumValueOf<T>(getString(key)) }.getOrDefault(fallback)

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) add(transform(getJSONObject(index)))
        }
    }
}
