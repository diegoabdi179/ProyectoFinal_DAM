package com.example.harvestdistributionapp.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.harvestdistributionapp.AppViewModel
import com.example.harvestdistributionapp.ui.screens.*

sealed class Screen(val route: String) {
    val pattern: String get() = route

    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object ForgotPassword : Screen("forgot_password")

    data object ProducerHome : Screen("producer_home")
    data object ProducerProducts : Screen("producer_products")
    data object ProducerLowStock : Screen("producer_low_stock")
    data object ProducerPublish : Screen("producer_publish")
    data object ProducerRequests : Screen("producer_requests")

    data object ProducerProductDetail : Screen("producer_product_detail/{productId}") {
        fun createRoute(productId: Int) = "producer_product_detail/$productId"
    }

    data object ProducerEditProduct : Screen("producer_edit_product/{productId}") {
        fun createRoute(productId: Int) = "producer_edit_product/$productId"
    }

    data object ProducerSuccess : Screen("producer_success/{productId}") {
        fun createRoute(productId: Int) = "producer_success/$productId"
    }

    data object ProducerRequestDetail : Screen("producer_request_detail/{requestId}") {
        fun createRoute(requestId: Int) = "producer_request_detail/$requestId"
    }

    data object BuyerHome : Screen("buyer_home")
    data object BuyerSearch : Screen("buyer_search")
    data object BuyerMyRequests : Screen("buyer_my_requests")
    data object BuyerPostNeed : Screen("buyer_post_need")

    data object BuyerProductDetail : Screen("buyer_product_detail/{productId}") {
        fun createRoute(productId: Int) = "buyer_product_detail/$productId"
    }

    data object BuyerRequest : Screen("buyer_request/{productId}") {
        fun createRoute(productId: Int) = "buyer_request/$productId"
    }

    data object BuyerRequestSent : Screen("buyer_request_sent/{requestId}") {
        fun createRoute(requestId: Int) = "buyer_request_sent/$requestId"
    }

    data object BuyerPostResults : Screen("buyer_post_results/{productName}/{quantity}/{date}/{location}") {
        fun createRoute(productName: String, quantity: Int, date: String, location: String) =
            "buyer_post_results/${Uri.encode(productName)}/$quantity/${Uri.encode(date)}/${Uri.encode(location)}"
    }

    data object ProducerProfile : Screen("producer_profile/{producerId}") {
        fun createRoute(producerId: String) = "producer_profile/${Uri.encode(producerId)}"
    }

    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    data object ProfileInfo : Screen("profile_info")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val state = uiState.appState

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController, uiState) }
        composable(Screen.Welcome.route) { WelcomeScreen(navController) }
        composable(Screen.Login.route) {
            val authViewModel: com.example.harvestdistributionapp.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            LoginScreen(navController, authViewModel)
        }
        composable(Screen.SignUp.route) {
            val authViewModel: com.example.harvestdistributionapp.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            SignUpScreen(navController, authViewModel)
        }
        composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }

        composable(Screen.ProducerHome.route) {
            val productViewModel: com.example.harvestdistributionapp.viewmodel.ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            ProducerHomeScreen(navController, state, productViewModel = productViewModel)
        }
        composable(Screen.ProducerProducts.route) {
            val productViewModel: com.example.harvestdistributionapp.viewmodel.ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            ProducerProductsScreen(navController, state, productViewModel = productViewModel)
        }
        composable(Screen.ProducerLowStock.route) {
            val productViewModel: com.example.harvestdistributionapp.viewmodel.ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            ProducerProductsScreen(navController, state, productViewModel = productViewModel, lowStockOnly = true)
        }
        composable(Screen.ProducerPublish.route) {
            val productViewModel: com.example.harvestdistributionapp.viewmodel.ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            ProducerPublishScreen(navController, state, productViewModel)
        }
        composable(Screen.ProducerRequests.route) { ProducerRequestsScreen(navController, state, viewModel) }
        composable(
            Screen.ProducerProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { entry -> ProducerProductDetailScreen(navController, state, entry.requireInt("productId")) }
        composable(
            Screen.ProducerEditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { entry ->
            val productViewModel: com.example.harvestdistributionapp.viewmodel.ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            ProducerEditProductScreen(navController, state, productViewModel, entry.requireInt("productId"))
        }
        composable(
            Screen.ProducerSuccess.route,
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { entry -> ProducerSuccessScreen(navController, state, entry.requireInt("productId")) }
        composable(
            Screen.ProducerRequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.IntType })
        ) { entry -> ProducerRequestDetailScreen(navController, state, viewModel, entry.requireInt("requestId")) }

        composable(Screen.BuyerHome.route) { BuyerHomeScreen(navController, state) }
        composable(Screen.BuyerSearch.route) { BuyerSearchScreen(navController, state) }
        composable(Screen.BuyerMyRequests.route) { BuyerMyRequestsScreen(navController, state) }
        composable(Screen.BuyerPostNeed.route) { BuyerPostNeedScreen(navController, state) }
        composable(
            Screen.BuyerProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { entry -> BuyerProductDetailScreen(navController, state, entry.requireInt("productId")) }
        composable(
            Screen.BuyerRequest.route,
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { entry -> BuyerRequestScreen(navController, state, viewModel, entry.requireInt("productId")) }
        composable(
            Screen.BuyerRequestSent.route,
            arguments = listOf(navArgument("requestId") { type = NavType.IntType })
        ) { entry -> BuyerRequestSentScreen(navController, state, entry.requireInt("requestId")) }
        composable(
            Screen.BuyerPostResults.route,
            arguments = listOf(
                navArgument("productName") { type = NavType.StringType },
                navArgument("quantity") { type = NavType.IntType },
                navArgument("date") { type = NavType.StringType },
                navArgument("location") { type = NavType.StringType }
            )
        ) { entry ->
            BuyerPostResultsScreen(
                navController = navController,
                state = state,
                productName = Uri.decode(entry.arguments?.getString("productName").orEmpty()),
                quantity = entry.requireInt("quantity"),
                date = Uri.decode(entry.arguments?.getString("date").orEmpty()),
                location = Uri.decode(entry.arguments?.getString("location").orEmpty())
            )
        }
        composable(
            Screen.ProducerProfile.route,
            arguments = listOf(navArgument("producerId") { type = NavType.StringType })
        ) { entry ->
            ProducerPublicProfileScreen(
                navController,
                state,
                Uri.decode(entry.arguments?.getString("producerId").orEmpty())
            )
        }

        composable(Screen.Profile.route) { ProfileScreen(navController, state, viewModel) }
        composable(Screen.EditProfile.route) {
            val authViewModel: com.example.harvestdistributionapp.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            EditProfileScreen(navController, authViewModel)
        }
        composable(Screen.ProfileInfo.route) { ProfileInfoScreen(navController, state) }
        composable(Screen.Settings.route) { SettingsScreen(navController, state, viewModel) }
    }
}

private fun androidx.navigation.NavBackStackEntry.requireInt(name: String): Int =
    requireNotNull(arguments?.getInt(name)) { "Falta el argumento de navegación '$name'" }
