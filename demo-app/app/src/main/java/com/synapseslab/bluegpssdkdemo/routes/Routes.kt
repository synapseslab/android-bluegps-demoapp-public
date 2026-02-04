package com.synapseslab.bluegpssdkdemo.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.synapseslab.bluegpssdkdemo.screens.init.InitScreen
import com.synapseslab.bluegpssdkdemo.screens.login.LoginScreen
import com.synapseslab.bluegpssdkdemo.screens.login.LoginViewModel
import com.synapseslab.bluegpssdkdemo.screens.main.MainScreen

@Composable
fun AppRoutes() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = viewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

    // This effect will react to changes in the login state and navigate accordingly.
    LaunchedEffect(isLoggedIn) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        when (isLoggedIn) {
            true -> {
                // Navigate to main only if we are not already there
                if (currentRoute != "main") {
                    navController.navigate("main") {
                        // Clear the back stack up to the init screen
                        popUpTo("init") { inclusive = true }
                    }
                }
            }
            false -> {
                 // Navigate to login only if we are not already there
                if (currentRoute != "login") {
                    navController.navigate("login") {
                        // Clear the entire back stack
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
            null -> {
                // Stay on the init screen while checking
            }
        }
    }

    NavHost(navController = navController, startDestination = "init") {
        composable("init") {
            InitScreen()
        }
        composable("login") {
            // The ViewModel is passed to allow the screen to call the login() function
            LoginScreen(loginViewModel = loginViewModel)
        }
        composable("main") {
            // The ViewModel is passed down to allow the Settings screen to call logout()
            MainScreen(loginViewModel = loginViewModel)
        }
    }
}