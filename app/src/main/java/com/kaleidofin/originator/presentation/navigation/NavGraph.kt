package com.kaleidofin.originator.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kaleidofin.originator.presentation.screen.CameraCaptureScreen
import com.kaleidofin.originator.presentation.screen.DynamicFormScreen
import com.kaleidofin.originator.presentation.screen.ForgotPasswordScreen
import com.kaleidofin.originator.presentation.screen.HomeScreen
import com.kaleidofin.originator.presentation.screen.ImagePreviewScreen
import com.kaleidofin.originator.presentation.screen.LoginScreen
import com.kaleidofin.originator.presentation.screen.QRScannerScreen
import com.kaleidofin.originator.presentation.screen.WebViewScreen
import com.kaleidofin.originator.domain.model.CameraConfig
import com.kaleidofin.originator.domain.model.QRConfig
import com.google.gson.Gson
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaleidofin.originator.presentation.viewmodel.DynamicFormViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object ForgotPassword : Screen("forgot_password")
    // SINGLE DynamicForm route for entire flow - no target parameter
    // Screen transitions happen via ViewModel state updates, not navigation
    object DynamicForm : Screen("dynamic_form/{flowId}/{productCode}/{partnerCode}/{branchCode}") {
        fun createRoute(
            flowId: String? = null,
            productCode: String? = null,
            partnerCode: String? = null,
            branchCode: String? = null
        ): String {
            // Use empty string as placeholder for null values (Navigation Compose doesn't support truly optional path parameters)
            val flowIdParam = flowId ?: ""
            val productCodeParam = productCode ?: ""
            val partnerCodeParam = partnerCode ?: ""
            val branchCodeParam = branchCode ?: ""
            return "dynamic_form/$flowIdParam/$productCodeParam/$partnerCodeParam/$branchCodeParam"
        }
    }
    object WebView : Screen("webview/{url}?title={title}") {
        fun createRoute(url: String, title: String = "External Process"): String {
            val encodedUrl = android.net.Uri.encode(url)
            val encodedTitle = android.net.Uri.encode(title)
            return "webview/$encodedUrl?title=$encodedTitle"
        }
    }
    object CameraCapture : Screen("camera_capture/{cameraConfig}") {
        fun createRoute(cameraConfig: String): String {
            return "camera_capture/${android.net.Uri.encode(cameraConfig)}"
        }
    }
    object ImagePreview : Screen("image_preview/{imageUri}") {
        fun createRoute(imageUri: String): String {
            return "image_preview/${android.net.Uri.encode(imageUri)}"
        }
    }
    object QRScanner : Screen("qr_scanner/{qrConfig}") {
        fun createRoute(qrConfig: String): String {
            return "qr_scanner/${android.net.Uri.encode(qrConfig)}"
        }
    }
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDynamicForm = { target, flowId, productCode, partnerCode, branchCode ->
                    // Navigate to DynamicFormScreen - single route for entire flow
                    // Screen transitions happen via ViewModel state, not navigation
                    navController.navigate(
                        Screen.DynamicForm.createRoute(
                            flowId = flowId,
                            productCode = productCode,
                            partnerCode = partnerCode,
                            branchCode = branchCode
                        )
                    )
                }
            )
        }
        
        composable(
            route = Screen.DynamicForm.route,
            arguments = listOf(
                navArgument("flowId") { type = NavType.StringType },
                navArgument("productCode") { type = NavType.StringType },
                navArgument("partnerCode") { type = NavType.StringType },
                navArgument("branchCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Convert empty string back to null
            fun String?.toNullable(): String? = if (this.isNullOrBlank()) null else this
            val flowId = backStackEntry.arguments?.getString("flowId")?.toNullable()
            val productCode = backStackEntry.arguments?.getString("productCode")?.toNullable()
            val partnerCode = backStackEntry.arguments?.getString("partnerCode")?.toNullable()
            val branchCode = backStackEntry.arguments?.getString("branchCode")?.toNullable()
            
            // SINGLE DynamicFormScreen for entire flow
            // Screen transitions happen via ViewModel state updates, not navigation
            DynamicFormScreen(
                flowId = flowId,
                productCode = productCode,
                partnerCode = partnerCode,
                branchCode = branchCode,
                navController = navController,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.WebView.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "External Process" }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: "External Process"
            WebViewScreen(
                url = android.net.Uri.decode(url),
                title = android.net.Uri.decode(title),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.CameraCapture.route,
            arguments = listOf(
                navArgument("cameraConfig") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val configJson = backStackEntry.arguments?.getString("cameraConfig") ?: ""
            val gson = Gson()
            val cameraConfig = try {
                gson.fromJson(android.net.Uri.decode(configJson), CameraConfig::class.java)
            } catch (e: Exception) {
                CameraConfig() // Default config
            }
            
            var capturedImageUri: android.net.Uri? by remember { mutableStateOf(null) }
            
            CameraCaptureScreen(
                cameraConfig = cameraConfig,
                onImageCaptured = { uri ->
                    // Set the captured image URI in the previous back stack entry (DynamicFormScreen)
                    // This allows DynamicCameraCaptureField to receive it via SavedStateHandle
                    navController.previousBackStackEntry?.savedStateHandle?.set("capturedImageUri", uri.toString())
                    // Pop only the camera screen to return to the form
                    navController.popBackStack()
                },
                onNavigateBack = {
                    // User pressed back button - just pop the camera screen
                    navController.popBackStack()
                },
                onNavigateToPreview = { uri ->
                    navController.navigate(Screen.ImagePreview.createRoute(uri.toString()))
                }
            )
        }
        
        composable(
            route = Screen.ImagePreview.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val imageUriStr = backStackEntry.arguments?.getString("imageUri") ?: ""
            val imageUri = android.net.Uri.parse(android.net.Uri.decode(imageUriStr))
            
            ImagePreviewScreen(
                imageUri = imageUri,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.QRScanner.route,
            arguments = listOf(
                navArgument("qrConfig") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val configJson = backStackEntry.arguments?.getString("qrConfig") ?: ""
            val gson = Gson()
            val qrConfig = try {
                gson.fromJson(android.net.Uri.decode(configJson), QRConfig::class.java)
            } catch (e: Exception) {
                QRConfig(format = "JSON", prefillMapping = emptyList()) // Default config
            }
            
            // Get ViewModel to call Aadhaar decode API
            val viewModel: DynamicFormViewModel = hiltViewModel()
            
            QRScannerScreen(
                qrConfig = qrConfig,
                onQRScanned = { prefillData ->
                    // Set the scanned data in the previous back stack entry (DynamicFormScreen)
                    navController.previousBackStackEntry?.savedStateHandle?.set("qrScannedData", prefillData)
                    navController.popBackStack()
                },
                onAadhaarQRDetected = { rawBytes ->
                    // Set the Aadhaar QR data in the previous back stack entry
                    navController.previousBackStackEntry?.savedStateHandle?.set("aadhaarQRData", rawBytes)
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDecodeAadhaarQR = { qrDataBase64 ->
                    // Return async coroutine that calls the decode API
                    kotlinx.coroutines.CoroutineScope(Dispatchers.Main).async {
                        viewModel.decodeAadhaarQR(qrDataBase64)
                    }
                }
            )
        }
    }
}


