package com.example.iptv.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.iptv.data.repository.XtreamRepository
import com.example.iptv.ui.login.LoginScreen
import com.example.iptv.ui.main.MainScreen

@Composable
fun IPTVNavigation(navController: NavHostController = rememberNavController()) {
        var repository: XtreamRepository? by remember { mutableStateOf(null) }

        NavHost(navController = navController, startDestination = Screen.Login.route) {
                composable(Screen.Login.route) {
                        LoginScreen(
                                onLoginSuccess = { repo ->
                                        repository = repo
                                        navController.navigate(Screen.Categories.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                }
                        )
                }

                composable(Screen.Categories.route) {
                        repository?.let { repo -> MainScreen(repository = repo) }
                }
        }
}
