package com.autoguard.companion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autoguard.companion.ui.screens.*
import com.autoguard.companion.ui.viewmodel.MainViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onNavigateToHome = {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("home") {
            HomeScreen(viewModel = viewModel, navController = navController)
        }
        composable("alerts") {
            AlertsScreen(viewModel = viewModel, navController = navController)
        }
        composable("alert_detail/{alertId}") { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId")?.toIntOrNull()
            AlertDetailScreen(alertId = alertId, viewModel = viewModel, navController = navController)
        }
        composable("find_bike") {
            FindBikeScreen(viewModel = viewModel, navController = navController)
        }
        composable("history") {
            HistoryScreen(viewModel = viewModel, navController = navController)
        }
        composable("profile") {
            ProfileScreen(viewModel = viewModel, navController = navController)
        }
        composable("contacts") {
            ContactsScreen(viewModel = viewModel, navController = navController)
        }
    }
}
