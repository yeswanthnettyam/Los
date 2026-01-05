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
    object DynamicForm : Screen("dynamic_form/{target}") {
        fun createRoute(target: String) = "dynamic_form/$target"
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
                onNavigateToDynamicForm = { target ->
                    navController.navigate(Screen.DynamicForm.createRoute(target))
                }
            )
        }
        
        composable(
            route = Screen.DynamicForm.route,
            arguments = listOf(navArgument("target") { type = NavType.StringType })
        ) { backStackEntry ->
            val target = backStackEntry.arguments?.getString("target") ?: ""
            DynamicFormScreen(
                target = target,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNext = { nextTarget ->
                    // Navigate to next screen without popping previous screens
                    // This maintains the back stack so back button works correctly
                    navController.navigate(Screen.DynamicForm.createRoute(nextTarget))
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


