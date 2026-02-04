package com.synapseslab.bluegpssdkdemo.screens.navigation

import android.content.res.Configuration
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synapseslab.bluegps_sdk.component.map.BlueGPSMapListener
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.data.model.map.AuthParameters
import com.synapseslab.bluegps_sdk.data.model.map.GenericInfo
import com.synapseslab.bluegps_sdk.data.model.map.JavascriptCallback
import com.synapseslab.bluegps_sdk.data.model.map.Position
import com.synapseslab.bluegps_sdk.data.model.map.TypeMapCallback
import com.synapseslab.bluegpssdkdemo.screens.map.MapViewManager
import com.synapseslab.bluegpssdkdemo.screens.map.TAG

private const val TAG = "NavigationScreen"

/**
 * A composable screen that displays a map and initiates navigation to a specified destination.
 *
 * This screen embeds the BlueGPS map view using [AndroidView] and leverages the [MapViewManager]
 * to control the map's behavior. Upon composition, it automatically triggers the navigation
 * from the user's current location to the provided [position].
 *
 * @param position The destination [Position] for the navigation.
 */
@Composable
fun NavigationScreen(
    position: Position
) {

    val context = LocalContext.current
    val mapViewManager = remember { MapViewManager.getInstance(context) }

    Text(text = position.toString())

    LaunchedEffect(Unit) {
        mapViewManager.gotoFromMe(position)
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxSize()) { // Fill max size for the map and FAB container
            AndroidView(
                modifier = Modifier
                    .alpha(0.99f)
                    .fillMaxSize(), // Fill max size for the map view
                factory = {
                    mapViewManager.apply { // Use the already obtained instance
                        // View -> Compose communication
                        setBlueGPSMapListener(object : BlueGPSMapListener {
                            override fun resolvePromise(
                                data: JavascriptCallback,
                                typeMapCallback: TypeMapCallback
                            ) {
                                when (typeMapCallback) {
                                    TypeMapCallback.INIT_SDK_COMPLETED -> {
                                        data.payload?.let { it1 -> Log.d(TAG, it1) }
                                        post {
                                            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                                                Configuration.UI_MODE_NIGHT_YES -> setDarkMode(true)
                                                Configuration.UI_MODE_NIGHT_NO -> setDarkMode(false)
                                                else -> setDarkMode(false)
                                            }

                                            // Initial state can be set here or via ViewModel

                                        }
                                    }

                                    TypeMapCallback.AUTH_ERROR -> {
                                        post {
                                            initAuth(AuthParameters(token = BlueGPSLib.instance.accessToken()))
                                        }
                                    }

                                    TypeMapCallback.SUCCESS -> {
                                        val cType =
                                            object : TypeToken<GenericInfo>() {}.type
                                        val payloadResponse =
                                            Gson().fromJson<GenericInfo>(
                                                data.payload,
                                                cType
                                            )
                                        payloadResponse.key = data.key
                                        Log.d(TAG, " $payloadResponse ")
                                    }

                                    TypeMapCallback.ERROR -> {
                                        val cType =
                                            object : TypeToken<GenericInfo>() {}.type
                                        val payloadResponse =
                                            Gson().fromJson<GenericInfo>(
                                                data.payload,
                                                cType
                                            )
                                        payloadResponse.key = data.key
                                        Log.e(TAG, " $payloadResponse ")
                                    }

                                    else -> {}
                                }
                            }
                        })
                    }.also {
                        if (it.parent != null) (it.parent as ViewGroup).removeView(it)
                    }
                },
                update = { view ->
                    // Compose -> View communication

                }
            )
        }
    }

}