package com.synapseslab.bluegpssdkdemo.screens.map

import android.content.res.Configuration
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synapseslab.bluegps_sdk.component.map.BlueGPSMapListener
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.data.model.map.AuthParameters
import com.synapseslab.bluegps_sdk.data.model.map.Floor
import com.synapseslab.bluegps_sdk.data.model.map.GenericInfo
import com.synapseslab.bluegps_sdk.data.model.map.JavascriptCallback
import com.synapseslab.bluegps_sdk.data.model.map.TypeMapCallback
import com.synapseslab.bluegpssdkdemo.ui.theme.Primary
import kotlinx.coroutines.launch

const val TAG = "MapScreen"

/**
 * Composable function for the Map screen.
 *
 * This screen displays an interactive map using the BlueGPS SDK. It integrates an [AndroidView]
 * to host the `MapViewManager` and provides UI controls for map interactions such as
 * changing floors, showing a specific tag, and toggling "follow me" mode.
 *
 * The screen observes [MapViewModel] for map commands and updates its UI accordingly.
 * It also handles callbacks from the BlueGPS map for initialization, authentication errors,
 * and other map events.
 *
 * @param mapViewModel The [MapViewModel] instance responsible for managing map-related state and logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
) { // Obtain ViewModel
    val context = LocalContext.current
    val mapViewManager = remember { MapViewManager.getInstance(context) } // Get instance once

    var showBottomSheet by remember { mutableStateOf(false) }
    var showMapCommand by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var floorList by remember { mutableStateOf<List<Floor>>(emptyList()) }
    val isFollowMeEnabled by mapViewModel.isFollowMeEnabled.collectAsState()

    // State for the dropdown menu
    var showMenu by remember { mutableStateOf(false) }

    // Observe map commands from ViewModel
    LaunchedEffect(Unit) {
        mapViewModel.mapCommands.collect { command ->
            when (command) {
                is MapCommand.ForceFollowMe -> mapViewManager.forceFollowMe(command.enable)
                is MapCommand.GetFloorList -> {
                    mapViewManager.getFloor { result, error ->
                        error?.let {
                            Log.e(TAG, "$error")
                        } ?: run {
                            result?.let {
                                floorList = result
                                showBottomSheet = true
                            }
                        }
                    }
                }

                is MapCommand.GotoFloor -> {
                    mapViewManager.gotoFloor(command.floor)
                }

                is MapCommand.ShowTag -> {
                    mapViewManager.showTag(command.tagId)
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "Select a floor",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )// Wrap in a Column
                LazyColumn(modifier = Modifier.weight(1f)) { // Give LazyColumn weight
                    items(floorList) { floor ->
                        Text(
                            text = floor.name ?: "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    mapViewModel.setFloor(floor)
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showBottomSheet = false
                                        }
                                    }
                                }
                                .padding(16.dp)
                        )
                    }
                }
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Close")
                }
            }
        }
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
                                        showMapCommand = true
                                        post {
                                            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                                                Configuration.UI_MODE_NIGHT_YES -> setDarkMode(true)
                                                Configuration.UI_MODE_NIGHT_NO -> setDarkMode(false)
                                                else -> setDarkMode(false)
                                            }

                                            // Initial state can be set here or via ViewModel
                                            // forceFollowMe(true)
                                            // enableRotateMap(true)

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
                }
            )

            // This Box will contain the FAB and the DropdownMenu, acting as their common parent
            // and allowing the DropdownMenu to implicitly anchor to the FAB's position within this scope.
            if (showMapCommand) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // Align this container Box to BottomEnd
                        .padding(16.dp) // Apply padding to the container
                ) {
                    FloatingActionButton(
                        onClick = { showMenu = !showMenu },
                        containerColor = Primary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Filled.MoreVert, "More actions")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        // Offset is relative to the top-left of the FAB (which is at 0,0 within this Box)
                        // FAB size is typically 56.dp. Estimated menu width ~180.dp, height ~150.dp.
                        // x-offset: fab.width - menu.width = 56.dp - 180.dp = -124.dp
                        // y-offset: -menu.height - padding = -150.dp - 8.dp = -158.dp
                        offset = DpOffset(x = (-124).dp, y = (-158).dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change floor") },
                            onClick = {
                                mapViewModel.getFloorList()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Layers, "Change floor") }
                        )
                        DropdownMenuItem(
                            text = { Text("Show tag") },
                            onClick = {
                                mapViewModel.showTag()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, "Show tag") }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isFollowMeEnabled) "Unfollow me" else "Follow me") },
                            onClick = {
                                mapViewModel.toggleFollowMe()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFollowMeEnabled) Icons.Filled.LocationOff else Icons.Filled.Navigation,
                                    contentDescription = if (isFollowMeEnabled) "Unfollow" else "Follow"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}