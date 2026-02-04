package com.synapseslab.bluegpssdkdemo.screens.main

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.data.model.map.Position
import com.synapseslab.bluegpssdkdemo.receiver.BeaconEventReceiver
import com.synapseslab.bluegpssdkdemo.screens.booking.BookingScreen
import com.synapseslab.bluegpssdkdemo.screens.login.LoginViewModel
import com.synapseslab.bluegpssdkdemo.screens.map.MapScreen
import com.synapseslab.bluegpssdkdemo.screens.navigation.NavigationScreen
import com.synapseslab.bluegpssdkdemo.screens.settings.SettingsScreen
import com.synapseslab.bluegpssdkdemo.screens.sse.SSEScreen

sealed class Screen(val route: String, val name: String, val icon: ImageVector) {
    object Map : Screen("map", name = "Map", Icons.Filled.Map)
    object SSE : Screen("sse", name = "SSE", Icons.Default.Loop)
    object Booking : Screen("booking", name = "Booking", Icons.Default.DateRange)
    object Settings : Screen("settings", name = "Settings", Icons.Default.Settings)
}

val items = listOf(
    Screen.Map,
    Screen.SSE,
    Screen.Booking,
    Screen.Settings,
)

private const val TAG = "MainScreen"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    loginViewModel: LoginViewModel = viewModel()
) {

    val context = LocalContext.current

    // Start tracking when the screen enters composition and stop when it leaves
    DisposableEffect(Unit) {
        startTrackingUserPosition(context)
        onDispose {
            stopTrackingUserPosition(context)
        }
    }

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
            composable(Screen.Settings.route) { SettingsScreen(loginViewModel = loginViewModel) }
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

/**
 * Starts the BlueGPS user position tracking service.
 * @param context The application context.
 */
fun startTrackingUserPosition(context: Context) {
    // Create the PendingIntent for background beacon events
    val beaconEventIntent = Intent(context, BeaconEventReceiver::class.java).apply {
        action = BlueGPSLib.ACTION_BEACON_EVENT // This is crucial for the receiver to work
    }

    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE  // MUTABLE allows fillInIntent data
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

    val beaconEventPendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        beaconEventIntent,
        pendingIntentFlags
    )

    BlueGPSLib.instance.startTrackingUserPosition(
        context,
        beaconEventPendingIntent,
        onError = {
            Log.e(TAG, "Error: $it")
        }
    )
}

/**
 * Stops the BlueGPS user position tracking service.
 * @param context The application context.
 */
fun stopTrackingUserPosition(context: Context) {
    BlueGPSLib.instance.stopTrackingUserPosition(context)
}