package com.example.harvestdistributionapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.harvestdistributionapp.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {
    @Test
    fun completeLocalWorkflowPersistsAndUpdatesExactEntities() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val repository = PersistentAppRepository(context)
        repository.resetForTesting()

        try {
            val producer = repository.signUp(
                name = "Productora Prueba",
                email = "productora@test.local",
                password = "Cosecha88",
                role = UserRole.PRODUCER
            ).requireSuccess()

            val product = repository.createProduct(
                ProductDraft(
                    name = "Calabaza de prueba",
                    quantity = 20,
                    unit = "kg",
                    pricePerUnit = 25.0,
                    location = "Toluca",
                    availableDate = futureIsoDate(1),
                    imageUri = "content://test/calabaza.jpg",
                    category = "verduras"
                )
            ).requireSuccess()
            assertEquals(producer.id, product.producerId)

            repository.logout()
            val buyer = repository.signUp(
                name = "Comprador Prueba",
                email = "comprador@test.local",
                password = "Mercado88",
                role = UserRole.BUYER
            ).requireSuccess()

            val request = repository.createRequest(
                RequestDraft(
                    productId = product.id,
                    quantity = 5,
                    requiredDate = futureIsoDate(2),
                    location = "Metepec",
                    message = "Entregar por la mañana"
                )
            ).requireSuccess()
            assertEquals(buyer.id, request.buyerId)
            assertEquals(product.id, request.productId)
            assertEquals(5, request.quantity)

            repository.logout()
            val login = repository.login("PRODUCTORA@test.local", "Cosecha88").requireSuccess()
            assertEquals(producer.id, login.id)
            val accepted = repository.updateRequestStatus(request.id, RequestStatus.ACCEPTED).requireSuccess()
            assertEquals(RequestStatus.ACCEPTED, accepted.status)

            val restored = PersistentAppRepository(context).state.first()
            assertEquals(producer.id, restored.currentUserId)
            assertEquals(15, restored.products.single { it.id == product.id }.quantity)
            assertEquals(RequestStatus.ACCEPTED, restored.requests.single { it.id == request.id }.status)
            assertFalse(restored.users.any { it.passwordHash == "Cosecha88" })
        } finally {
            repository.resetForTesting()
        }
    }

    @Test
    fun repositoryRejectsDuplicateAccountsAndInvalidStockRequests() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val repository = PersistentAppRepository(context)
        repository.resetForTesting()

        try {
            repository.signUp("Cuenta Inicial", "cuenta@test.local", "Cosecha88", UserRole.BUYER).requireSuccess()
            assertTrue(repository.signUp("Duplicada", "CUENTA@test.local", "Cosecha99", UserRole.BUYER) is AppResult.Error)

            val firstProduct = repository.state.first().products.first()
            val tooLarge = repository.createRequest(
                RequestDraft(firstProduct.id, firstProduct.quantity + 1, futureIsoDate(2), "Toluca", "")
            )
            assertTrue(tooLarge is AppResult.Error)
        } finally {
            repository.resetForTesting()
        }
    }

    private fun <T> AppResult<T>.requireSuccess(): T = when (this) {
        is AppResult.Success -> value
        is AppResult.Error -> throw AssertionError(message)
    }
}
