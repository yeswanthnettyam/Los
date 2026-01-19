package com.kaleidofin.originator.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kaleidofin.originator.presentation.screen.DynamicFormScreen
import com.kaleidofin.originator.presentation.screen.ForgotPasswordScreen
import com.kaleidofin.originator.presentation.screen.HomeScreen
import com.kaleidofin.originator.presentation.screen.LoginScreen

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
    }
}


