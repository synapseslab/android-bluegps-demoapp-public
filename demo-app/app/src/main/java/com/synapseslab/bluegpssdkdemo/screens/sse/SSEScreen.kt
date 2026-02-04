package com.synapseslab.bluegpssdkdemo.screens.sse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapseslab.bluegpssdkdemo.ui.theme.Primary

private const val TAG = "SSEScreen"

/**
 * Composable function for the Server-Sent Events (SSE) screen.
 *
 * This screen displays real-time updates for region enter/exit events and beacon scans.
 * It provides a user interface to visualize logs from the SSE service and allows clearing these logs.
 *
 * @param sseViewModel The [SSEViewModel] instance used to manage the screen's state and logic.
 *                     It is provided by the `viewModel()` delegate.
 */
@Composable
fun SSEScreen(
    sseViewModel: SSEViewModel = viewModel()
) {
    val uiState by sseViewModel.uiState.collectAsState()

    // Scroll state used for the Beacon log area
    val scrollState = rememberScrollState()
    // Separate scroll state for the Region log area to avoid interfering with the beacon log scrolling
    val scrollStateForRegion = rememberScrollState()

    // Automatically scroll the beacon status text to the bottom when new content is added.
    LaunchedEffect(uiState.beaconStatus) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Automatically scroll the region log to the bottom when new Enter/Exit messages arrive
    LaunchedEffect(uiState.regionStatus) {
        scrollStateForRegion.animateScrollTo(scrollStateForRegion.maxValue)
    }

    // Start tracking when the screen enters composition and stop when it leaves
    DisposableEffect(Unit) {
        sseViewModel.startNotifyRegion() // Start region notification when screen is active
        onDispose {
            sseViewModel.stopNotifyRegion() // Stop region notification when screen is disposed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Server Sent Events Screen",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // --- Region Log Section ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .weight(0.5f) // Split vertical space 50/50 with the Beacon Log below
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Region Log",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                TextButton(
                    onClick = { sseViewModel.clearRegionLogs() },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Primary,
                        containerColor = Color.Transparent
                    )
                ) {
                    Text("Clear")
                }
            }
            Card(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollStateForRegion) // Scroll container for region logs
                ) {
                    Text(
                        text = uiState.regionStatus,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // --- Beacon Log Section ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .weight(0.5f) // Split vertical space 50/50 with the Region Log above
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Beacon Scan Log",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                TextButton(
                    onClick = { sseViewModel.clearBeaconLogs() },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Primary,
                        containerColor = Color.Transparent
                    )
                ) {
                    Text("Clear")
                }
            }
            Card(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState) // Scroll container for beacon logs
                ) {
                    Text(
                        text = uiState.beaconStatus,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}


