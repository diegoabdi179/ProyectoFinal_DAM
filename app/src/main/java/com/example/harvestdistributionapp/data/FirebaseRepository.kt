package com.example.harvestdistributionapp.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
     * Guarda un producto en la colección "productos" de Firestore.
     */
    suspend fun guardarProduct(product: Product): Result<String> {
        return try {
            Log.d(TAG, "Guardando Product en Firestore...")
            val generatedId = if (product.id <= 0) System.currentTimeMillis().toInt().absoluteValue else product.id
            val docRef = firestore.collection("productos").document(generatedId.toString())
            val productConId = product.copy(id = generatedId)
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
            val productConId = product.copy(id = productId)
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
}
