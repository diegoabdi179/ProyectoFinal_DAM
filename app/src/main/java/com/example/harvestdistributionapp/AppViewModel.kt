package com.example.harvestdistributionapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.harvestdistributionapp.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val appState: AppState = AppState(),
    val isReady: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = PersistentAppRepository(application.applicationContext)

    val uiState: StateFlow<AppUiState> = repository.state
        .map { AppUiState(appState = it, isReady = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    fun signUp(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        onResult: (AppResult<UserAccount>) -> Unit
    ) = launchResult({ repository.signUp(name, email, password, role) }, onResult)

    fun login(
        email: String,
        password: String,
        onResult: (AppResult<UserAccount>) -> Unit
    ) = launchResult({ repository.login(email, password) }, onResult)

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.logout()
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {}
            onComplete()
        }
    }

    fun updateProfile(
        name: String,
        location: String,
        businessName: String,
        onResult: (AppResult<UserAccount>) -> Unit
    ) = launchResult({ repository.updateProfile(name, location, businessName) }, onResult)

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch { repository.updateSettings(settings) }
    }

    fun createProduct(draft: ProductDraft, onResult: (AppResult<Product>) -> Unit) =
        launchResult({ repository.createProduct(draft) }, onResult)

    fun updateProduct(productId: Int, draft: ProductDraft, onResult: (AppResult<Product>) -> Unit) =
        launchResult({ repository.updateProduct(productId, draft) }, onResult)

    fun createRequest(draft: RequestDraft, onResult: (AppResult<PurchaseRequest>) -> Unit) =
        launchResult({ repository.createRequest(draft) }, onResult)

    fun updateRequestStatus(
        requestId: Int,
        status: RequestStatus,
        onResult: (AppResult<PurchaseRequest>) -> Unit
    ) = launchResult({ repository.updateRequestStatus(requestId, status) }, onResult)

    private fun <T> launchResult(
        block: suspend () -> AppResult<T>,
        onResult: (AppResult<T>) -> Unit
    ) {
        viewModelScope.launch { onResult(block()) }
    }
}
