package com.synapseslab.bluegpssdkdemo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.synapseslab.bluegpssdkdemo.advertising.AdvManager
import com.synapseslab.bluegpssdkdemo.routes.AppRoutes
import com.synapseslab.bluegpssdkdemo.ui.theme.BlueGPSDemoAppTheme
import com.synapseslab.bluegpssdkdemo.ui.theme.Primary

/**
 * Main entry activity. Sets up Bluetooth prerequisites, requests runtime permissions,
 * binds the advertising service, and hosts the Compose navigation graph.
 */
class MainActivity : ComponentActivity() {

    // Bluetooth references (null when BLE not supported)
    private var btAdapter: BluetoothAdapter? = null
    private var btManager: BluetoothManager? = null

    // Launcher for enabling Bluetooth via system dialog (must be initialized via registerForActivityResult)
    // NOTE: In this sample it's declared but never initialized. Consider using
    // bluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private var bluetoothLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for BLE support and retrieve the adapter if available
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            btAdapter = btManager!!.adapter
        }

        // Ask user to enable Bluetooth if supported but disabled
        if (btAdapter != null && !btAdapter!!.isEnabled) {
            val blueToothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothLauncher?.launch(blueToothIntent) // Requires proper initialization of bluetoothLauncher
        }

        // Bind the BlueGPS advertising service early so that advertising can be started later
        AdvManager.bindBlueGPSAdvertisingService(applicationContext)

        setContent {
            BlueGPSDemoAppTheme {
                val context = LocalContext.current

                // Foreground permissions are dynamic based on the Android version
                val foregroundPermissions = remember {
                    mutableListOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            add(Manifest.permission.BLUETOOTH_CONNECT)
                            add(Manifest.permission.BLUETOOTH_ADVERTISE)
                            add(Manifest.permission.BLUETOOTH_SCAN)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                }

                // Background location permission (only relevant from Android Q)
                val backgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                } else null

                // Track whether all required permissions have been granted
                var hasAllPermissions by remember {
                    val foregroundGranted = foregroundPermissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    val backgroundGranted = backgroundPermission?.let {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    } ?: true
                    mutableStateOf(foregroundGranted && backgroundGranted)
                }

                // Launcher for background location permission
                val backgroundPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            hasAllPermissions = true
                        }
                    }
                )

                // Launcher for foreground permissions; chains background permission when needed
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        val foregroundGranted = permissions.values.all { it }
                        if (foregroundGranted) {
                            backgroundPermission?.let {
                                backgroundPermissionLauncher.launch(it)
                            } ?: run {
                                hasAllPermissions = true
                            }
                        }
                    }
                )

                // Main scaffold: either show the app content or a rationale to request permissions
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (hasAllPermissions) {
                        AppRoutes()
                    } else {
                        // Ask user to grant permissions required for BLE scanning and notifications
                        PermissionRationaleUI(innerPadding = innerPadding) {
                            permissionLauncher.launch(foregroundPermissions)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Unbind the advertising service to avoid leaks when the activity stops
        AdvManager.unBindBlueGPSAdvertisingService(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure advertising is stopped when the activity is destroyed
        AdvManager.stopAdvertising()
    }
}

/**
 * Simple UI prompting the user to grant the required runtime permissions.
 */
@Composable
private fun PermissionRationaleUI(innerPadding: PaddingValues, onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val buttonColors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = Color.White
        )

        Text("This app requires several permissions to function correctly, including location and Bluetooth access. Please grant the required permissions.")
        Button(onClick = onGrantClick, modifier = Modifier.padding(top = 8.dp), colors = buttonColors) {
            Text("Grant Permissions")
        }
    }
}
