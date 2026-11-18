/*
 * Copyright (c) 2026 Synapses s.r.l. All rights reserved.
 */

package com.synapseslab.bluegpssdkdemo.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapseslab.bluegps_sdk.background.SessionState
import com.synapseslab.bluegpssdkdemo.session.DemoSession
import com.synapseslab.bluegpssdkdemo.ui.theme.Primary

/**
 * Background session (SDK 6.4.0+): one ongoing notification, drawn by the app, under which
 * advertising and the location work keep running in the background.
 */
@Composable
fun SessionScreen(viewModel: SessionViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val indicator by viewModel.indicator.collectAsState()
    val limitations by viewModel.limitations.collectAsState()
    val advertising by viewModel.advertising.collectAsState()
    val content by viewModel.content.collectAsState()
    val events by viewModel.events.collectAsState()
    val message by viewModel.message.collectAsState()
    val errors by viewModel.errors.collectAsState()

    val colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
    val running = state is SessionState.Running

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Background session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.start(context) }, enabled = !running, colors = colors) {
                Text("Start session")
            }
            Button(onClick = { viewModel.stop() }, enabled = running, colors = colors) {
                Text("Stop session")
            }
        }
        OutlinedButton(onClick = { viewModel.update() }, enabled = running) {
            Text("Update notification (${content.updates})")
        }

        Text(
            "Start session starts the session, beacon scan and advertising together. " +
                "Stop session, the Stop of the notification and the logout stop all of them.",
            style = MaterialTheme.typography.bodySmall,
        )

        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        errors.forEach { Text(it, color = MaterialTheme.colorScheme.error) }

        HorizontalDivider()
        Line("Session", state.toString())
        Line("Notification", indicator.toString())
        Line("Limitations", if (limitations.isEmpty()) "none" else limitations.joinToString())
        Line("Beacon scan", if (content.beaconScan) "on" else "off")
        Line("Advertising", DemoSession.describe(advertising))

        HorizontalDivider()
        Text("Events", style = MaterialTheme.typography.titleMedium)
        if (events.isEmpty()) Text("No events yet", style = MaterialTheme.typography.bodySmall)
        events.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
