package com.example.harvestdistributionapp.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harvestdistributionapp.data.FirebaseRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 1. Sealed class genérica 'UiState' para gestionar los estados visuales de la interfaz.
 */
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T, val rol: String = "") : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/**
 * 2. AuthViewModel optimizado para gestionar exclusivamente los 4 campos del formulario de registro:
 * nombre, email, password y rol ("productor" o "comprador").
 */
class AuthViewModel(
    private val repository: FirebaseRepository = FirebaseRepository(),
) : ViewModel() {

    companion object {
        private const val TAG = "AuthVM"
    }

    // Campos de texto para formulario de registro y login
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _rol = MutableStateFlow("") // Empieza vacío, NUNCA por defecto como "comprador"
    val rol: StateFlow<String> = _rol.asStateFlow()

    private val _nombreUsuario = MutableStateFlow("")
    val nombreUsuario: StateFlow<String> = _nombreUsuario.asStateFlow()

    init {
        fetchUserData()
    }

    fun fetchUserData() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                try {
                    val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(uid)
                        .get()
                        .await()
                    if (doc.exists()) {
                        _rol.value = doc.getString("rol") ?: ""
                        _nombreUsuario.value = doc.getString("nombre") ?: ""
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Estado visual de la UI de autenticación
    private val _authState = MutableStateFlow<UiState<FirebaseUser>>(UiState.Idle)
    val authState: StateFlow<UiState<FirebaseUser>> = _authState.asStateFlow()

    // Actualizadores de campos
    fun onEmailChanged(value: String) {
        _email.value = value
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
    }

    fun onNombreChanged(value: String) {
        _nombre.value = value
    }

    fun onRolChanged(value: String) {
        _rol.value = value
    }

    fun resetState() {
        Log.d(TAG, "resetState() llamado. Estado devuelto a UiState.Idle")
        _authState.value = UiState.Idle
    }

    /**
     * Función para iniciar sesión con validaciones funcionales.
     */
    fun loginUser() {
        val emailStr = _email.value.trim()
        val passwordStr = _password.value
        Log.d(TAG, "loginUser() disparado para email: $emailStr")

        when {
            emailStr.isBlank() || passwordStr.isBlank() -> {
                Log.w(TAG, "Validación login fallida: Campos vacíos")
                _authState.value = UiState.Error("Los campos no pueden estar vacíos")
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(emailStr).matches() -> {
                Log.w(TAG, "Validación login fallida: Email con formato incorrecto ($emailStr)")
                _authState.value = UiState.Error("El formato del correo electrónico no es válido")
                return
            }
            passwordStr.length < 6 -> {
                Log.w(TAG, "Validación login fallida: Contraseña menor a 6 caracteres")
                _authState.value = UiState.Error("La contraseña debe tener al menos 6 caracteres")
                return
            }
        }

        Log.d(TAG, "Validación login exitosa. Emitiendo UiState.Loading...")
        _authState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = repository.iniciarSesion(emailStr, passwordStr)) {
                is com.example.harvestdistributionapp.data.Result.Success -> {
                    val uid = result.data.uid
                    var rolDb = "comprador"
                    var nombreDb = ""
                    try {
                        val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("usuarios")
                            .document(uid)
                            .get()
                            .await()
                        rolDb = doc.getString("rol") ?: "comprador"
                        nombreDb = doc.getString("nombre") ?: ""
                    } catch (_: Exception) {}
                    _rol.value = rolDb
                    _nombreUsuario.value = nombreDb
                    Log.i(TAG, "Login exitoso para UID: $uid con rol: $rolDb, nombre: $nombreDb. Emitiendo UiState.Success")
                    _authState.value = UiState.Success(result.data, rolDb)
                }
                is com.example.harvestdistributionapp.data.Result.Error -> {
                    Log.e(TAG, "Error en repositorio al iniciar sesión: ${result.exception.message}", result.exception)
                    _authState.value = UiState.Error(
                        result.exception.localizedMessage ?: "Error desconocido al iniciar sesión"
                    )
                }
            }
        }
    }

    /**
     * 1 y 2. Función para registrar usuario gestionando exclusivamente los 4 campos solicitados.
     */
    fun registerUser() {
        val nombreStr = _nombre.value.trim()
        val emailStr = _email.value.trim()
        val passwordStr = _password.value
        val rolStr = _rol.value
        Log.d(TAG, "registerUser() disparado. Nombre: '$nombreStr', Email: '$emailStr', Rol: '$rolStr'")

        // Validación de campos
        when {
            nombreStr.isBlank() || emailStr.isBlank() || passwordStr.isBlank() -> {
                Log.w(TAG, "Validación registro fallida: Campos obligatorios vacíos")
                _authState.value = UiState.Error("Los campos obligatorios no pueden estar vacíos")
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(emailStr).matches() -> {
                Log.w(TAG, "Validación registro fallida: Email con formato incorrecto ($emailStr)")
                _authState.value = UiState.Error("El formato del correo electrónico no es válido")
                return
            }
            passwordStr.length < 6 -> {
                Log.w(TAG, "Validación registro fallida: Contraseña menor a 6 caracteres")
                _authState.value = UiState.Error("La contraseña debe tener al menos 6 caracteres")
                return
            }
        }

        Log.d(TAG, "Validación registro exitosa. Emitiendo UiState.Loading y llamando a FirebaseRepository...")
        _authState.value = UiState.Loading
        viewModelScope.launch {
            // Se envían únicamente los 4 parámetros; el repositorio maneja los valores por defecto
            when (val result = repository.registrarUsuario(
                nombre = nombreStr,
                correo = emailStr,
                contrasena = passwordStr,
                rol = rolStr
            )) {
                is com.example.harvestdistributionapp.data.Result.Success -> {
                    Log.i(TAG, "Registro exitoso para UID: ${result.data.uid} con rol: $rolStr. Emitiendo UiState.Success")
                    _authState.value = UiState.Success(result.data, rolStr)
                }
                is com.example.harvestdistributionapp.data.Result.Error -> {
                    Log.e(TAG, "Error al registrar usuario: ${result.exception.message}", result.exception)
                    _authState.value = UiState.Error(
                        result.exception.localizedMessage ?: "Error desconocido al registrar usuario"
                    )
                }
            }
        }
    }

    /**
     * Actualiza los datos del perfil en Firestore (colección 'usuarios/{uid}').
     */
    fun updateFirestoreProfile(
        nombre: String,
        ubicacion: String,
        negocio: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onResult(false, "No hay una sesión activa")
            return
        }
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "nombre" to nombre,
                    "ubicacion" to ubicacion,
                    "negocio" to negocio
                )
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .update(updates)
                    .await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Error al actualizar perfil en Firestore")
            }
        }
    }
}
