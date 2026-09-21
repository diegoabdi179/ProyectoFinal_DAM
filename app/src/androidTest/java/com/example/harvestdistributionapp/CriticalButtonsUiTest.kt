package com.example.harvestdistributionapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.harvestdistributionapp.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriticalButtonsUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var repository: PersistentAppRepository

    @Before
    fun resetApplication() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        repository = PersistentAppRepository(context)
        runBlocking { repository.resetForTesting() }
        composeRule.activityRule.scenario.recreate()
        waitForTag("welcome_start")
    }

    @After
    fun clearApplication() {
        runBlocking { repository.resetForTesting() }
    }

    @Test
    fun buyerCanFindExactProductRequestMinimumQuantityEditProfileAndLogout() {
        composeRule.onNodeWithTag("welcome_start").performClick()
        waitForTag("signup_name")
        composeRule.onNodeWithTag("signup_name").performTextInput("Compradora UI")
        composeRule.onNodeWithTag("signup_email").performTextInput("compradora.ui@test.local")
        composeRule.onNodeWithTag("signup_password").performTextInput("Mercado88")
        composeRule.onNodeWithTag("role_buyer").performScrollTo().performClick()
        composeRule.onNodeWithTag("signup_submit").performScrollTo().performClick()

        waitForTag("home_search")
        composeRule.onNodeWithTag("home_search").performClick()
        waitForTag("buyer_search_input")
        composeRule.onNodeWithTag("buyer_search_input").performTextInput("Lechuga")
        waitForTag("product_card_2")
        composeRule.onNodeWithTag("product_card_1").assertDoesNotExist()
        composeRule.onNodeWithTag("product_card_2").performClick()

        waitForTag("product_detail_name")
        composeRule.onNodeWithTag("product_detail_name").assertTextEquals("Lechuga Orejona")
        composeRule.onNodeWithTag("request_product").performClick()
        waitForTag("request_quantity_value")
        composeRule.onNodeWithTag("request_quantity_minus").performClick()
        composeRule.onNodeWithText("1 piezas").assertExists()
        composeRule.onNodeWithTag("request_submit").performScrollTo().performClick()

        waitForTag("request_sent_title")
        composeRule.onNodeWithTag("request_sent_title").assertTextEquals("Solicitud enviada")
        composeRule.onNodeWithTag("view_my_requests").performClick()
        waitForTag("buyer_request_1")
        composeRule.onNodeWithTag("buyer_request_1").assertExists()

        composeRule.onNodeWithTag("buyer_nav_profile").performClick()
        waitForTag("profile_edit")
        composeRule.onNodeWithTag("profile_edit").performClick()
        waitForTag("edit_profile_name")
        composeRule.onNodeWithTag("edit_profile_name").performTextReplacement("Compradora Editada")
        composeRule.onNodeWithTag("edit_profile_save").performScrollTo().performClick()
        waitForTag("profile_name")
        composeRule.onNodeWithTag("profile_name").assertTextEquals("Compradora Editada")

        composeRule.onNodeWithTag("profile_settings").performClick()
        waitForTag("theme_dark")
        composeRule.onNodeWithTag("theme_dark").performClick()
        composeRule.onNodeWithTag("notifications_setting").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            runBlocking {
                repository.state.first().settings == AppSettings(ThemeMode.DARK, notificationsEnabled = false)
            }
        }
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("profile_logout")
        composeRule.onNodeWithTag("profile_logout").performClick()
        waitForTag("welcome_login")
        assertNull(runBlocking { repository.state.first().currentUserId })
    }

    @Test
    fun producerButtonsOpenLowStockHandleTwoExactRequestsAndEditExactProduct() {
        lateinit var product: Product
        lateinit var firstRequest: PurchaseRequest
        lateinit var secondRequest: PurchaseRequest
        runBlocking {
            repository.signUp("Productora UI", "productora.ui@test.local", "Cosecha88", UserRole.PRODUCER).requireSuccess()
            product = repository.createProduct(
                ProductDraft(
                    "Calabaza UI",
                    20,
                    "kg",
                    25.0,
                    "Toluca",
                    futureIsoDate(1),
                    "content://test/calabaza-ui.jpg",
                    "verduras"
                )
            ).requireSuccess()
            repository.logout()
            repository.signUp("Comprador UI", "comprador.ui@test.local", "Mercado88", UserRole.BUYER).requireSuccess()
            firstRequest = repository.createRequest(RequestDraft(product.id, 5, futureIsoDate(2), "Metepec", "Primera")).requireSuccess()
            secondRequest = repository.createRequest(RequestDraft(product.id, 3, futureIsoDate(3), "Toluca", "Segunda")).requireSuccess()
            repository.logout()
        }

        composeRule.onNodeWithTag("welcome_login").performClick()
        waitForTag("login_email")
        composeRule.onNodeWithTag("login_email").performTextInput("productora.ui@test.local")
        composeRule.onNodeWithTag("login_password").performTextInput("Cosecha88")
        composeRule.onNodeWithTag("login_submit").performClick()

        waitForTag("stat_low_stock")
        composeRule.onNodeWithTag("stat_low_stock").performClick()
        waitForTag("producer_product_${product.id}")
        composeRule.onNodeWithTag("producer_product_${product.id}").assertExists()
        composeRule.onNodeWithContentDescription("Regresar").performClick()

        waitForTag("stat_requests")
        composeRule.onNodeWithTag("stat_requests").performClick()
        waitForTag("producer_request_${firstRequest.id}")
        composeRule.onNodeWithTag("accept_request_${firstRequest.id}").performClick()
        composeRule.onNodeWithTag("reject_request_${secondRequest.id}").performClick()
        waitForTag("confirm_reject_${secondRequest.id}")
        composeRule.onNodeWithTag("confirm_reject_${secondRequest.id}").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking {
                val state = repository.state.first()
                state.requests.single { it.id == firstRequest.id }.status == RequestStatus.ACCEPTED &&
                    state.requests.single { it.id == secondRequest.id }.status == RequestStatus.REJECTED &&
                    state.products.single { it.id == product.id }.quantity == 15
            }
        }

        composeRule.onNodeWithTag("producer_nav_producer_products").performClick()
        waitForTag("edit_product_${product.id}")
        composeRule.onNodeWithTag("edit_product_${product.id}").performClick()
        waitForTag("product_quantity")
        composeRule.onNodeWithTag("product_quantity").performScrollTo().performTextReplacement("30")
        composeRule.onNodeWithTag("product_submit").performScrollTo().performClick()
        waitForTag("producer_detail_name")
        composeRule.onNodeWithTag("producer_detail_name").assertTextEquals("Calabaza UI")
        composeRule.waitUntil(5_000) {
            runBlocking { repository.state.first().products.single { it.id == product.id }.quantity == 30 }
        }
    }

    private fun waitForTag(tag: String, timeoutMillis: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun <T> AppResult<T>.requireSuccess(): T = when (this) {
        is AppResult.Success -> value
        is AppResult.Error -> throw AssertionError(message)
    }
}
