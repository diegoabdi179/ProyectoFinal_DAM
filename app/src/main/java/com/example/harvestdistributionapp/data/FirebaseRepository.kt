package com.example.harvestdistributionapp.data

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.math.absoluteValue

/**
 * 1. Sealed class Result para manejo tipado de respuestas exitosas o errores.
 */
sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(val exception: Exception) : Result<Nothing>
}

/**
 * 2. Data classes requeridas para Usuario, Producto y Solicitud.
 */
data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: String = "comprador", // "productor" o "comprador"
    val ubicacion: String = "",
    val negocio: String = "",
)

data class Producto(
    val id: String = "",
    val productorId: String = "",
    val productorNombre: String = "",
    val nombre: String = "",
    val cantidad: Int = 0,
    val unidad: String = "",
    val precioPorUnidad: Double = 0.0,
    val categoria: String = "",
    val ubicacion: String = "",
    val fechaDisponible: String = "",
    val imagenUri: String = "",
)

data class Solicitud(
    val id: String = "",
    val compradorId: String = "",
    val compradorNombre: String = "",
    val productoId: String = "",
    val productorId: String = "",
    val productoNombre: String = "",
    val cantidad: Int = 0,
    val unidad: String = "",
    val precioPorUnidad: Double = 0.0,
    val total: Double = 0.0,
    val fechaRequerida: String = "",
    val estado: String = "PENDIENTE",
    val mensaje: String = "",
    val fechaCreacionEpoch: Long = System.currentTimeMillis(),
)



/**
 * 3. Repositorio de Firebase que utiliza inyección manual de dependencias
 * y corrutinas con .await() de kotlinx-coroutines-play-services.
 */
class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    companion object {
        private const val TAG = "FirebaseRepo"
    }

    /**
     * Registra un usuario con Firebase Auth y guarda sus datos adicionales en la colección 'usuarios' de Firestore.
     */
    suspend fun registrarUsuario(
        nombre: String,
        correo: String,
        contrasena: String,
        rol: String,
        ubicacion: String = "",
        negocio: String = ""
    ): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Intentando crear usuario en Auth para: $correo")
            // 1. Crear cuenta en Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(correo, contrasena).await()
            val firebaseUser = authResult.user ?: throw Exception("No se pudo obtener el usuario autenticado")
            val uid = firebaseUser.uid
            Log.d(TAG, "Usuario creado exitosamente en Auth con UID: $uid")

            // 2. Construir objeto de datos del usuario
            val usuario = Usuario(
                uid = uid,
                nombre = nombre,
                correo = correo,
                rol = rol,
                ubicacion = ubicacion,
                negocio = negocio
            )

            // 3. Guardar datos adicionales en Firestore ('usuarios/{uid}')
            Log.d(TAG, "Intentando guardar documento en Firestore colección 'usuarios/$uid'")
            firestore.collection("usuarios")
                .document(uid)
                .set(usuario)
                .await()
            Log.d(TAG, "Documento guardado exitosamente en Firestore")

            Result.Success(firebaseUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar usuario: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Inicia sesión de un usuario con Firebase Auth.
     */
    suspend fun iniciarSesion(correo: String, contrasena: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Intentando iniciar sesión para: $correo")
            val authResult = auth.signInWithEmailAndPassword(correo, contrasena).await()
            val firebaseUser = authResult.user ?: throw Exception("No se pudo autenticar al usuario")
            Log.d(TAG, "Inicio de sesión exitoso para UID: ${firebaseUser.uid}")
            Result.Success(firebaseUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar sesión: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Guarda un nuevo 'Producto' en la colección 'productos' de Firestore.
     */
    suspend fun guardarProducto(producto: Producto): Result<String> {
        return try {
            Log.d(TAG, "Guardando producto en Firestore...")
            val docRef = firestore.collection("productos").document()
            val productoConId = producto.copy(id = docRef.id)
            docRef.set(productoConId).await()
            Log.d(TAG, "Producto guardado con ID: ${docRef.id}")
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar producto: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Guarda una nueva 'Solicitud' en la colección 'solicitudes' de Firestore.
     */
    suspend fun guardarSolicitud(solicitud: Solicitud): Result<String> {
        return try {
            Log.d(TAG, "Guardando solicitud en Firestore...")
            val docRef = firestore.collection("solicitudes").document()
            val solicitudConId = solicitud.copy(id = docRef.id)
            docRef.set(solicitudConId).await()
            Log.d(TAG, "Solicitud guardada con ID: ${docRef.id}")
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar solicitud: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Guarda una solicitud en la colección "solicitudes" de Firestore.
     */
    suspend fun guardarSolicitudFirestore(request: PurchaseRequest): Result<String> {
        return try {
            Log.d(TAG, "Guardando PurchaseRequest en Firestore...")
            val generatedId = if (request.id <= 0) System.currentTimeMillis().toInt().absoluteValue else request.id
            val docRef = firestore.collection("solicitudes").document(generatedId.toString())
            val requestConId = request.copy(id = generatedId)
            docRef.set(requestConId).await()
            Log.d(TAG, "PurchaseRequest guardada con ID: $generatedId")
            Result.Success(generatedId.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar PurchaseRequest: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Escucha en tiempo real las solicitudes donde el 'producerId' coincide con el usuario actual.
     */
    fun obtenerSolicitudesProductorRealtime(producerId: String): Flow<Result<List<PurchaseRequest>>> = callbackFlow {
        val listener = firestore.collection("solicitudes")
            .whereEqualTo("producerId", producerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = mutableListOf<PurchaseRequest>()
                    for (doc in snapshot.documents) {
                        try {
                            val req = doc.toObject(PurchaseRequest::class.java)
                            if (req != null) {
                                requests.add(req)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapeando solicitud del productor: ${e.message}", e)
                        }
                    }
                    trySend(Result.Success(requests))
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Escucha en tiempo real las solicitudes donde el 'buyerId' coincide con el usuario actual.
     */
    fun obtenerSolicitudesCompradorRealtime(buyerId: String): Flow<Result<List<PurchaseRequest>>> = callbackFlow {
        val listener = firestore.collection("solicitudes")
            .whereEqualTo("buyerId", buyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = mutableListOf<PurchaseRequest>()
                    for (doc in snapshot.documents) {
                        try {
                            val req = doc.toObject(PurchaseRequest::class.java)
                            if (req != null) {
                                requests.add(req)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapeando solicitud del comprador: ${e.message}", e)
                        }
                    }
                    trySend(Result.Success(requests))
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Actualiza el estado de una solicitud en Firestore.
     */
    suspend fun actualizarEstadoSolicitud(requestId: Int, newStatus: RequestStatus): Result<Unit> {
        return try {
            val docRef = firestore.collection("solicitudes").document(requestId.toString())
            docRef.update("status", newStatus).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Guarda un producto en la colección "productos" de Firestore.
     */
    suspend fun guardarProduct(product: Product): Result<String> {
        return try {
            Log.d(TAG, "Guardando Product en Firestore...")
            val generatedId = if (product.id <= 0) System.currentTimeMillis().toInt().absoluteValue else product.id
            val docRef = firestore.collection("productos").document(generatedId.toString())

            val currentUser = FirebaseAuth.getInstance().currentUser
            var realProducerName = currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: currentUser?.email?.substringBefore("@")
                ?: "Productor"

            if (currentUser != null) {
                try {
                    val userDoc = firestore.collection("usuarios").document(currentUser.uid).get().await()
                    val nombreDb = userDoc.getString("nombre")
                    if (!nombreDb.isNullOrBlank()) {
                        realProducerName = nombreDb
                    }
                } catch (_: Exception) {}
            }

            val finalProducerName = if (product.producerName.isBlank()) realProducerName else product.producerName

            val productConId = product.copy(
                id = generatedId,
                producerName = finalProducerName,
                productorId = if (product.producerId.isBlank()) currentUser?.uid.orEmpty() else product.producerId,
                producerId = if (product.producerId.isBlank()) currentUser?.uid.orEmpty() else product.producerId
            )
            docRef.set(productConId).await()
            Log.d(TAG, "Product guardado con ID: $generatedId")
            Result.Success(generatedId.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar Product: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Actualiza un producto existente en la colección "productos" de Firestore usando su ID original.
     */
    suspend fun actualizarProduct(productId: Int, product: Product): Result<String> {
        return try {
            Log.d(TAG, "Actualizando Product en Firestore con ID: $productId...")
            val docRef = firestore.collection("productos").document(productId.toString())

            val currentUser = FirebaseAuth.getInstance().currentUser
            var realProducerName = currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: currentUser?.email?.substringBefore("@")
                ?: "Productor"

            if (currentUser != null) {
                try {
                    val userDoc = firestore.collection("usuarios").document(currentUser.uid).get().await()
                    val nombreDb = userDoc.getString("nombre")
                    if (!nombreDb.isNullOrBlank()) {
                        realProducerName = nombreDb
                    }
                } catch (_: Exception) {}
            }

            val finalProducerName = if (product.producerName.isBlank()) realProducerName else product.producerName

            val productConId = product.copy(
                id = productId,
                producerName = finalProducerName,
                productorId = if (product.productorId.isBlank()) currentUser?.uid.orEmpty() else product.productorId,
                producerId = if (product.producerId.isBlank()) currentUser?.uid.orEmpty() else product.producerId
            )
            docRef.set(productConId, com.google.firebase.firestore.SetOptions.merge()).await()
            Log.d(TAG, "Product actualizado con ID: $productId")
            Result.Success(productId.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar Product: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Obtiene la lista de productos en tiempo real desde la colección "productos" usando addSnapshotListener.
     */
    fun obtenerProductosRealtime(): Flow<Result<List<Product>>> = callbackFlow {
        val listener = firestore.collection("productos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val products = snapshot.toObjects(Product::class.java)
                    trySend(Result.Success(products))
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Sube una imagen local a Cloudinary y retorna la URL pública segura.
     */
    suspend fun subirImagenCloudinary(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            MediaManager.get().upload(uri)
                .unsigned("app_cosecha")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Cloudinary upload started: $requestId")
                    }
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                        val secureUrl = resultData["secure_url"]?.toString().orEmpty()
                        Log.d(TAG, "Cloudinary upload success: $secureUrl")
                        if (continuation.isActive) {
                            continuation.resume(secureUrl)
                        }
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Cloudinary upload error: ${error.description}")
                        if (continuation.isActive) {
                            continuation.resume("")
                        }
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "Cloudinary upload rescheduled: ${error.description}")
                        if (continuation.isActive) {
                            continuation.resume("")
                        }
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Cloudinary upload dispatch: ${e.message}", e)
            if (continuation.isActive) {
                continuation.resume("")
            }
        }
    }
}
