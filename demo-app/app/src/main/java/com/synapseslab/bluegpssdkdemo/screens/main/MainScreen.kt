package com.synapseslab.bluegpssdkdemo.screens.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.synapseslab.bluegps_sdk.data.model.map.Position
import com.synapseslab.bluegpssdkdemo.screens.booking.BookingScreen
import com.synapseslab.bluegpssdkdemo.screens.legacy.LegacyAdvertisingScreen
import com.synapseslab.bluegpssdkdemo.screens.login.LoginViewModel
import com.synapseslab.bluegpssdkdemo.screens.map.MapScreen
import com.synapseslab.bluegpssdkdemo.screens.navigation.NavigationScreen
import com.synapseslab.bluegpssdkdemo.screens.session.SessionScreen
import com.synapseslab.bluegpssdkdemo.screens.settings.SettingsScreen
import com.synapseslab.bluegpssdkdemo.screens.sse.SSEScreen

sealed class Screen(val route: String, val name: String, val icon: ImageVector) {
    object Map : Screen("map", name = "Map", Icons.Filled.Map)
    object SSE : Screen("sse", name = "SSE", Icons.Default.Loop)
    object Booking : Screen("booking", name = "Booking", Icons.Default.DateRange)
    object Session : Screen("session", name = "Session", Icons.Default.Notifications)
    object Settings : Screen("settings", name = "Settings", Icons.Default.Settings)
}

val items = listOf(
    Screen.Map,
    Screen.SSE,
    Screen.Booking,
    Screen.Session,
    Screen.Settings,
)


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    loginViewModel: LoginViewModel = viewModel()
) {

    // Beacon scanning is not started here: it runs under the background session (Session tab,
    // session/DemoSession.kt), so that the app shows one notification for all its background work.

    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.name) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Map.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.Booking.route) {
                BookingScreen(
                    navigateToNavigationScreen = { position ->
                        val positionJson = Gson().toJson(position)
                        navController.navigate("navigation/$positionJson")
                    }
                )
            }
            composable(Screen.SSE.route) { SSEScreen() }
            composable(Screen.Session.route) { SessionScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    loginViewModel = loginViewModel,
                    onOpenLegacyAdvertising = { navController.navigate("legacy_advertising") },
                )
            }
            composable("legacy_advertising") { LegacyAdvertisingScreen() }
            composable(
                "navigation/{position}",
                arguments = listOf(navArgument("position") { type = NavType.StringType })
            ) { backStackEntry ->
                val positionJson = backStackEntry.arguments?.getString("position")
                val position = Gson().fromJson(positionJson, Position::class.java)
                NavigationScreen(position = position)
            }
        }
    }
}
